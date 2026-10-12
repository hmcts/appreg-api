package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.fee.enabled=true",
            "appreg.csds.ingress.processors.fee.mock=",
            "appreg.csds.ingress.processors.fee.parameters=",
            "appreg.csds.ingress.processors.fee.backup-source=",
            "appreg.csds.ingress.processors.fee.backup-target=",
            "appreg.csds.ingress.processors.fee.ingress-target=fee",
            "appreg.csds.ingress.processors.fee.primary-keys[0]=fee_reference",
            "appreg.csds.ingress.processors.fee.primary-keys[1]=fee_start_date",
            "appreg.csds.ingress.processors.application-codes.enabled=false",
            "appreg.csds.ingress.processors.resolution-codes.enabled=false",
            "appreg.csds.ingress.processors.standard-applicants.enabled=false",
            "appreg.csds.ingress.processors.national-court-houses.enabled=false",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}"
        })
class FeeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long STORED_ID = 9009901L;
    @Autowired private FeeDataIngressProcessor processor;
    @Autowired private CsdsIngressProcessor ingress;
    @Autowired private DatabaseJobRepository jobs;
    @Autowired private JdbcTemplate jdbc;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @BeforeEach
    void seedStoredFee() {
        cleanFees();
        jdbc.update(
                """
                INSERT INTO %s.fee (fee_id, fee_reference, fee_description, fee_value,
                    fee_start_date, fee_version, fee_changed_by, fee_changed_date, fee_user_name, is_offsite)
                VALUES (?, 'CSFEE', 'Old fee', 10, '2020-01-01', 1, 0, current_timestamp, 'TEST', false)
                """
                        .formatted(schema),
                STORED_ID);
    }

    @AfterEach
    void cleanFees() {
        jdbc.update(
                "DELETE FROM "
                        + schema
                        + ".app_list_entry_fee_id WHERE fee_fee_id IN (9009901, 9009902, 989001, 989002)");
        jdbc.update(
                "DELETE FROM "
                        + schema
                        + ".fee WHERE fee_reference IN ('CSFEE', 'BADFEE') OR fee_id=9009902");
    }

    @Test
    @Transactional
    void pipelinePreservesReferencesAndAuditsCompositeIdentityAcrossRepeatIngress()
            throws Exception {
        var job = jobs.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);
        if (job == null) {
            job = DatabaseJob.builder().name(CsdsIngressProcessor.DATABASE_JOB_NAME).build();
        }
        job.setEnabled(YesOrNo.YES);
        job.setMetadata(null);
        job.setLastRan(null);
        jobs.save(job);
        jdbc.update(
                "UPDATE "
                        + schema
                        + ".configuration_parameters SET parameter_value='DEBUG' WHERE parameter_name='AUDIT_CSDS'");
        var entryId =
                jdbc.queryForObject(
                        "SELECT min(ale_id) FROM " + schema + ".application_list_entries",
                        Long.class);
        jdbc.update(
                """
                INSERT INTO %s.app_list_entry_fee_id (ale_ale_id, fee_fee_id, version,
                    changed_by, changed_date, user_name)
                VALUES (?, ?, 1, 0, current_timestamp, 'TEST')
                """
                        .formatted(schema),
                entryId,
                STORED_ID);
        var old = source(Long.MAX_VALUE, null, "CSFEE", "2020-01-01").put("FEE_ID", 1L);
        var newer = source(889001L, null, "CSFEE", "2021-01-01");
        var offsite =
                source(null, 9009902L, "CO1.1", "1901-01-01")
                        .put("EndDate", "1901-12-31")
                        .putNull("RevisionNumber");
        var future = source(Long.MAX_VALUE, null, "CSFEE", "9999-01-01");
        stubFor(
                get(urlEqualTo("/count/APPREGISTER/CivilFee/GD"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"count\":4}")));
        stubFor(
                get(urlEqualTo("/query/APPREGISTER/CivilFee/GD?%24limit=4&%24offset=0"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(MAPPER.writeValueAsString(page(old, newer)))));
        stubFor(
                get(urlEqualTo("/query/APPREGISTER/CivilFee/GD?%24limit=2&%24offset=2"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                MAPPER.writeValueAsString(page(offsite, future)))));
        assertThat(ingress.runIngress()).isTrue();
        assertThat(ids("CSFEE")).containsExactly(STORED_ID, 989001L);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT fee_fee_id FROM "
                                        + schema
                                        + ".app_list_entry_fee_id WHERE ale_ale_id=? AND fee_fee_id=?",
                                Long.class,
                                entryId,
                                STORED_ID))
                .isEqualTo(STORED_ID);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT is_offsite FROM " + schema + ".fee WHERE fee_id=9009902",
                                Boolean.class))
                .isTrue();
        // Normalise integer-node widths after the JSON round trip; compare every payload field.
        assertThat(auditedSource(STORED_ID)).isEqualTo(MAPPER.readTree(old.toString()));
        assertThat(auditedSource(989001L)).isEqualTo(MAPPER.readTree(newer.toString()));
        final var before = stableSnapshot();
        for (var record : List.of(old, newer, offsite)) {
            record.remove(List.of("CivilFeeID", "PSSFixedListID"));
        }
        var repeated = processor.ingest(List.of(page(old, newer, offsite, future)));
        assertThat(repeated.getInserted()).isZero();
        assertThat(repeated.getUpdated()).isEqualTo(3);
        assertThat(stableSnapshot()).isEqualTo(before);
    }

    @Test
    void invalidBatchesLeaveEveryStoredColumnUnchanged() {
        var before = snapshot();
        var valid = source(889001L, null, "CSFEE", "2021-01-01");
        var missing = source(null, null, "BADFEE", "2020-01-01");
        var overflow = source(Long.MAX_VALUE, null, "BADFEE", "2020-01-01");
        for (var invalid :
                List.of(
                        valid.deepCopy().put("CivilFeeID", 889002L),
                        missing,
                        overflow,
                        source(8909901L, null, "BADFEE", "2020-01-01"),
                        source(889002L, null, "BADFEE", "invalid"),
                        source(889002L, null, "BADFEE", "2020-01-01").putNull("StartDate"),
                        source(889002L, null, "BADFEE", "9999-01-01")
                                .put("EndDate", "2026-02-30"))) {
            assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(invalid))))
                    .isInstanceOf(
                            invalid == overflow
                                    ? ArithmeticException.class
                                    : RuntimeException.class);
            assertThat(snapshot()).isEqualTo(before);
        }
    }

    @Test
    void allFutureBatchDoesNotEndDateMissingFees() {
        var before = snapshot();
        var response =
                processor.ingest(
                        List.of(page(source(Long.MAX_VALUE, null, "CSFEE", "9999-01-01"))));
        assertThat(response.getInserted()).isZero();
        assertThat(response.getUpdated()).isZero();
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void failedUpsertRollsBackTheEntireBatch() {
        var before = snapshot();
        var valid = source(889001L, null, "CSFEE", "2021-01-01");
        var oversized =
                source(889002L, null, "BADFEE", "2020-01-01").put("Description", "x".repeat(10001));
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid, oversized))))
                .isInstanceOf(CsdsBatchUpsertException.class);
        assertThat(snapshot()).isEqualTo(before);
    }

    private List<Long> ids(String reference) {
        return jdbc.queryForList(
                "SELECT fee_id FROM "
                        + schema
                        + ".fee WHERE fee_reference=? ORDER BY fee_start_date",
                Long.class,
                reference);
    }

    private JsonNode auditedSource(long id) throws Exception {
        var json =
                jdbc.queryForObject(
                        "SELECT csds_json::text FROM "
                                + schema
                                + ".csds_audit WHERE appreg_key=? ORDER BY ca_id DESC LIMIT 1",
                        String.class,
                        id);
        return MAPPER.readTree(json);
    }

    private List<Map<String, Object>> snapshot() {
        return jdbc.queryForList("SELECT * FROM " + schema + ".fee ORDER BY fee_id");
    }

    private List<Map<String, Object>> stableSnapshot() {
        return jdbc.queryForList(
                "SELECT fee_id, fee_reference, fee_start_date, fee_end_date, fee_value, "
                        + "fee_description, fee_version, is_offsite FROM "
                        + schema
                        + ".fee ORDER BY fee_id");
    }

    private JsonNode page(JsonNode... records) {
        var page = MAPPER.createObjectNode();
        var array = page.putArray("records");
        for (var record : records) {
            array.add(record);
        }
        return page;
    }

    private ObjectNode source(Long civilId, Long pssId, String reference, String start) {
        return MAPPER.createObjectNode()
                .put("CivilFeeID", civilId)
                .put("PSSFixedListID", pssId)
                .put("FeeReference", reference)
                .put("Description", "Updated fee")
                .put("FeeValue", "12.50")
                .put("StartDate", start)
                .putNull("EndDate")
                .put("RevisionNumber", 2L);
    }
}
