package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.page-size=1",
            "appreg.csds.ingress.processors.national-court-houses.enabled=true",
            "appreg.csds.ingress.processors.national-court-houses.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.national-court-houses.mock=",
            "appreg.csds.ingress.processors.national-court-houses.parameters=",
            "appreg.csds.ingress.processors.national-court-houses.backup-source=",
            "appreg.csds.ingress.processors.national-court-houses.backup-target=",
            "appreg.csds.ingress.processors.national-court-houses.ingress-target=national_court_houses",
            "appreg.csds.ingress.processors.national-court-houses.primary-keys[0]=courthouse_name",
            "appreg.csds.ingress.processors.application-code.enabled=false",
            "appreg.csds.ingress.processors.resolution-code.enabled=false",
            "appreg.csds.ingress.processors.standard-applicants.enabled=false",
            "appreg.csds.ingress.processors.fee.enabled=false",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}",
            "appreg.csds.ingress.access-keys[0]=primary-test-key",
            "appreg.csds.ingress.access-keys[1]=secondary-test-key"
        })
class NationalCourtHouseDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private CsdsIngressProcessor csdsIngressProcessor;
    @Autowired private DatabaseJobRepository databaseJobRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NationalCourtHouseDataIngressProcessor courthouseProcessor;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @BeforeEach
    void setUpJobRowAndExistingCourtHouse() {
        var databaseJob = databaseJobRepository.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);
        if (databaseJob == null) {
            databaseJob =
                    databaseJobRepository.save(
                            DatabaseJob.builder()
                                    .name(CsdsIngressProcessor.DATABASE_JOB_NAME)
                                    .enabled(YesOrNo.YES)
                                    .build());
        }
        databaseJob.setEnabled(YesOrNo.YES);
        databaseJob.setMetadata(null);
        databaseJob.setLastRan(null);
        databaseJobRepository.save(databaseJob);

        jdbcTemplate.update(
                "DELETE FROM "
                        + schema
                        + ".national_court_houses WHERE courthouse_name IN "
                        + "('Old Court','New Court','PSS Court','Expired Court')");
        jdbcTemplate.update(
                """
                INSERT INTO %s.national_court_houses (
                    nch_id, courthouse_name, version_number, changed_by, changed_date,
                    court_type, start_date, court_location_code
                ) VALUES (?, ?, ?, ?, current_timestamp, ?, ?, ?)
                """
                        .formatted(schema),
                9003106L,
                "Old Court",
                1L,
                0L,
                "CHOA",
                LocalDate.of(1900, 1, 1),
                "OLD");
    }

    @Test
    void given_nationalCourtHouseProcessorEnabled_when_runIngress_then_updatesAndInsertsRows()
            throws JsonProcessingException {
        var incomingRecords =
                List.of(
                        sourceRecord(3802L, 9999L, "Old Court", null, "B01CF00", 2L),
                        sourceRecord(3803L, null, "New Court", "Llys Newydd", "B02CF00", 1L));

        stubFor(
                get(urlPathEqualTo("/count/COURT/Court/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"count\":2}")));
        for (var offset = 0; offset < incomingRecords.size(); offset++) {
            var page = OBJECT_MAPPER.createObjectNode();
            page.putArray("records").add(incomingRecords.get(offset));
            stubFor(
                    get(urlEqualTo(
                                    "/query/COURT/Court/GD?%24limit="
                                            + (incomingRecords.size() - offset)
                                            + "&%24offset="
                                            + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key"))
                            .willReturn(
                                    aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(OBJECT_MAPPER.writeValueAsString(page))));
        }

        assertThat(csdsIngressProcessor.runIngress()).isTrue();
        assertThat(loadCourtHouses())
                .containsExactly(
                        new StagedCourtHouse(
                                103803L, "New Court", 1L, "CHOA", "B02CF00", "Llys Newydd"),
                        new StagedCourtHouse(9003106L, "Old Court", 2L, "CHOA", "B01CF00", null));
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT count(*)
                                FROM %s.national_court_houses
                                WHERE nch_id IN (9003106, 103803)
                                  AND (loc_loc_id IS NOT NULL OR psa_psa_id IS NOT NULL OR norg_id IS NOT NULL)
                                """
                                        .formatted(schema),
                                Integer.class))
                .isZero();

        verify(
                getRequestedFor(urlPathEqualTo("/count/COURT/Court/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key")));
        for (var offset = 0; offset < incomingRecords.size(); offset++) {
            verify(
                    getRequestedFor(
                                    urlEqualTo(
                                            "/query/COURT/Court/GD?%24limit="
                                                    + (incomingRecords.size() - offset)
                                                    + "&%24offset="
                                                    + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key")));
        }
    }

    @Test
    @Transactional
    void existingNameKeepsItsIdAndCodeConsumersAcrossRepeatedIngress() throws Exception {
        final var before = snapshot().size();
        var listId =
                jdbcTemplate.queryForObject(
                        "SELECT min(al_id) FROM " + schema + ".application_lists", Long.class);
        assertThat(listId).isNotNull();
        assertThat(
                        jdbcTemplate.update(
                                "UPDATE "
                                        + schema
                                        + ".application_lists SET courthouse_code='OLD001' WHERE al_id=?",
                                listId))
                .isEqualTo(1);
        jdbcTemplate.update(
                "UPDATE "
                        + schema
                        + ".configuration_parameters SET parameter_value='DEBUG' WHERE parameter_name='AUDIT_CSDS'");
        var changed = record(Long.MAX_VALUE, 9999L, "Old Court", 2L);
        var missing = changed.deepCopy();
        missing.remove(List.of("CourtID", "PSSNationalCourthouseID"));
        for (var source : List.of(changed, missing)) {
            courthouseProcessor.ingest(List.of(page(source)));
            assertThat(snapshot()).hasSize(before);
            var stored =
                    jdbcTemplate.queryForMap(
                            "SELECT nch_id, version_number, court_location_code FROM "
                                    + schema
                                    + ".national_court_houses WHERE courthouse_name='Old Court'");
            assertThat(stored.get("nch_id")).isEqualTo(9003106L);
            assertThat(((Number) stored.get("version_number")).longValue()).isEqualTo(2L);
            assertThat(stored.get("court_location_code")).isEqualTo("OLD001");
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT courthouse_code FROM "
                                            + schema
                                            + ".application_lists WHERE al_id=?",
                                    String.class,
                                    listId))
                    .isEqualTo("OLD001");
            assertThat(source.has("NCH_ID")).isFalse();
        }
        var audit =
                jdbcTemplate.queryForMap(
                        "SELECT appreg_key, csds_json FROM "
                                + schema
                                + ".csds_audit WHERE appreg_key='9003106' ORDER BY ca_id DESC LIMIT 1");
        assertThat(((Number) audit.get("appreg_key")).longValue()).isEqualTo(9003106L);
        var auditedSource = OBJECT_MAPPER.readTree(audit.get("csds_json").toString());
        assertThat(auditedSource.get("CourtName").textValue()).isEqualTo("Old Court");
        assertThat(auditedSource.has("CourtID")).isFalse();
        assertThat(auditedSource.has("NCH_ID")).isFalse();
    }

    @Test
    @Transactional
    void newNamesUseBothAllocationPathsRetainExpiredAndIgnoreFutureRecords() {
        var before = snapshot().size();
        var fallback = record(3803L, null, "New Court", 1L);
        var pss = record(Long.MAX_VALUE, 5506L, "PSS Court", 1L).put("CourtWelshName", "Llys");
        var expired =
                record(3805L, 5507L, "Expired Court", 1L)
                        .put("StartDate", "2020-01-01")
                        .put("EndDate", "2021-01-01");
        var future =
                record(Long.MAX_VALUE, 103803L, "New Court", 1L)
                        .put(
                                "StartDate",
                                LocalDate.now(ZoneId.of("Europe/London")).plusDays(1).toString());
        var pages = List.of(page(fallback, pss), page(expired, future));
        for (int i = 0; i < 2; i++) {
            courthouseProcessor.ingest(pages);
            assertThat(snapshot()).hasSize(before + 3);
            var ids =
                    jdbcTemplate.queryForList(
                            "SELECT nch_id FROM "
                                    + schema
                                    + ".national_court_houses WHERE courthouse_name IN "
                                    + "('New Court','PSS Court','Expired Court')",
                            Long.class);
            assertThat(ids).containsExactlyInAnyOrder(103803L, 5506L, 5507L);
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT sl_courthouse_name FROM "
                                            + schema
                                            + ".national_court_houses WHERE nch_id=5506",
                                    String.class))
                    .isEqualTo("Llys");
            assertThat(
                            jdbcTemplate.queryForObject(
                                    "SELECT end_date FROM "
                                            + schema
                                            + ".national_court_houses WHERE nch_id=5507",
                                    LocalDate.class))
                    .isEqualTo(LocalDate.parse("2021-01-01"));
        }
    }

    @Test
    void invalidBatchesAcrossPagesLeaveAllStoredCourtsUnchanged() {
        var before = snapshot();
        var valid = record(3803L, null, "New Court", 1L);
        var missingId = record(null, null, "Missing IDs", 1L);
        missingId.remove(List.of("CourtID", "PSSNationalCourthouseID"));
        var invalidRecords =
                List.of(
                        record(3804L, null, "New Court", 1L),
                        missingId,
                        record(Long.MAX_VALUE, null, "Overflow", 1L),
                        record(3804L, 9003106L, "Stored ID collision", 1L),
                        record(3804L, 103803L, "Incoming ID collision", 1L),
                        record(3804L, null, "Bad start date", 1L).put("StartDate", "not-a-date"),
                        record(3804L, null, "Missing start date", 1L).putNull("StartDate"),
                        record(3804L, null, "Bad future end date", 1L)
                                .put("StartDate", "9999-01-01")
                                .put("EndDate", "2026-02-30"));
        for (var invalid : invalidRecords) {
            assertThatThrownBy(
                            () -> courthouseProcessor.ingest(List.of(page(valid), page(invalid))))
                    .isInstanceOf(RuntimeException.class);
            assertThat(snapshot()).isEqualTo(before);
        }
    }

    @Test
    void allFutureInputDoesNotChangeOrEndDateMissingCourts() {
        var before = snapshot();
        var future = record(Long.MAX_VALUE, null, "Old Court", 1L).put("StartDate", "9999-01-01");
        courthouseProcessor.ingest(List.of(page(future)));
        assertThat(snapshot()).isEqualTo(before);
    }

    private ObjectNode record(Long courtId, Long pssId, String name, Long version) {
        return sourceRecord(courtId, pssId, name, null, "OLD001", version);
    }

    private JsonNode page(JsonNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var array = page.putArray("records");
        for (var record : records) {
            array.add(record);
        }
        return page;
    }

    private List<Map<String, Object>> snapshot() {
        return jdbcTemplate.queryForList(
                "SELECT nch_id, courthouse_name, version_number, start_date, end_date, "
                        + "court_location_code, sl_courthouse_name FROM "
                        + schema
                        + ".national_court_houses ORDER BY nch_id");
    }

    private List<StagedCourtHouse> loadCourtHouses() {
        return jdbcTemplate.query(
                """
                SELECT nch_id, courthouse_name, version_number, court_type,
                       court_location_code, sl_courthouse_name
                FROM %s.national_court_houses
                WHERE nch_id IN (9003106, 103803)
                ORDER BY nch_id
                """
                        .formatted(schema),
                (resultSet, rowNumber) ->
                        new StagedCourtHouse(
                                resultSet.getLong("nch_id"),
                                resultSet.getString("courthouse_name"),
                                resultSet.getLong("version_number"),
                                resultSet.getString("court_type"),
                                resultSet.getString("court_location_code"),
                                resultSet.getString("sl_courthouse_name")));
    }

    private ObjectNode sourceRecord(
            Long courtId,
            Long pssNationalCourtHouseId,
            String name,
            String welshName,
            String locationCode,
            Long revisionNumber) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CourtID", courtId)
                        .put("CourtName", name)
                        .put("CourtLocationCode", locationCode)
                        .put("StartDate", "1900-01-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", revisionNumber);
        record.put("PSSNationalCourthouseID", pssNationalCourtHouseId);
        record.put("CourtWelshName", welshName);
        return record;
    }

    private record StagedCourtHouse(
            Long id,
            String name,
            Long version,
            String courtType,
            String locationCode,
            String welshName) {}
}
