package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.page-size=2",
            "appreg.csds.ingress.processors.application-codes.enabled=true",
            "appreg.csds.ingress.processors.application-codes.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.application-codes.mock=",
            "appreg.csds.ingress.processors.application-codes.parameters=",
            "appreg.csds.ingress.processors.application-codes.table-name=application_codes",
            "appreg.csds.ingress.processors.application-codes.primary-key=ac_id",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}",
            "appreg.csds.ingress.access-keys[0]=primary-test-key",
            "appreg.csds.ingress.access-keys[1]=secondary-test-key"
        })
class ApplicationCodeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private CsdsIngressProcessor csdsIngressProcessor;
    @Autowired private ApplicationCodeDataIngressProcessor applicationCodeDataIngressProcessor;
    @Autowired private DatabaseJobRepository databaseJobRepository;
    @Autowired private ApplicationCodeRepository applicationCodeRepository;
    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    private LogCaptor applicationCodeDiffReportingLogs;
    private LogCaptor pagedProcessorLogs;

    @BeforeEach
    void setUpJobRowAndApplicationCodes() {
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

        applicationCodeDiffReportingLogs =
                LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        applicationCodeDiffReportingLogs.clearLogs();
        pagedProcessorLogs = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        pagedProcessorLogs.clearLogs();
        jdbcTemplate.update(
                "DELETE FROM %s.csds_audit".formatted(schema), new MapSqlParameterSource());
    }

    @Test
    void given_applicationCodeProcessorEnabled_when_runIngress_then_logsDiffSummary()
            throws JsonProcessingException {
        var existingApplicationCodes =
                applicationCodeRepository.findAll().stream()
                        .sorted(Comparator.comparing(ApplicationCode::getId))
                        .toList();
        var updated = existingApplicationCodes.get(1);
        var unmatched = existingApplicationCodes.get(2);

        updated.setEndDate(null);
        unmatched.setEndDate(null);
        applicationCodeRepository.saveAll(List.of(updated, unmatched));

        var incomingRecords = new ArrayList<>(List.of(toSourceRecordWithPssacid(updated, 9001L)));

        var insertedId =
                existingApplicationCodes.stream()
                                .map(ApplicationCode::getId)
                                .max(Long::compareTo)
                                .orElseThrow()
                        + 1000;
        incomingRecords.add(createInsertedRecord(insertedId));
        var totalCount = incomingRecords.size();

        stubFor(
                get(urlPathEqualTo("/count/CSDS/ApplicationCode/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"count\":%d}".formatted(totalCount))));

        for (var offset = 0; offset < totalCount; offset += 2) {
            var recordsPage = OBJECT_MAPPER.createObjectNode();
            var records = recordsPage.putArray("records");
            incomingRecords.stream().skip(offset).limit(2).forEach(records::add);

            stubFor(
                    get(urlEqualTo("/query/CSDS/ApplicationCode/GD?%24limit=2&%24offset=" + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key"))
                            .willReturn(
                                    aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    OBJECT_MAPPER.writeValueAsString(
                                                            recordsPage))));
        }

        var executed = csdsIngressProcessor.runIngress();
        var databaseJob = databaseJobRepository.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);
        var expectedPageCount = (totalCount + 1) / 2;
        var expectedUpdates = 1;

        assertThat(executed).isTrue();
        assertThat(databaseJob.getMetadata()).isNull();
        assertThat(databaseJob.getLastRan()).isNull();
        assertThat(pagedProcessorLogs.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved " + expectedPageCount + " CSDS pages"));
        assertThat(applicationCodeDiffReportingLogs.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "incoming=%d, existing=%d, inserts=1, updates=%d"
                                                .formatted(
                                                        totalCount,
                                                        existingApplicationCodes.size(),
                                                        expectedUpdates)));

        verify(
                getRequestedFor(urlPathEqualTo("/count/CSDS/ApplicationCode/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key")));
        for (var offset = 0; offset < totalCount; offset += 2) {
            verify(
                    getRequestedFor(
                                    urlEqualTo(
                                            "/query/CSDS/ApplicationCode/GD?%24limit=2&%24offset="
                                                    + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key")));
        }
    }

    @Test
    void given_debugAuditLevel_when_manualIngestSucceeds_then_batchesSuccessAudits() {
        setAuditLevel("DEBUG");

        var insertedId =
                applicationCodeRepository.findAll().stream()
                                .map(ApplicationCode::getId)
                                .max(Long::compareTo)
                                .orElseThrow()
                        + 1000;

        var response =
                applicationCodeDataIngressProcessor.ingest(
                        List.of(page(createInsertedRecord(insertedId))));

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isZero();

        var audits =
                jdbcTemplate.queryForList(
                        """
                        SELECT appreg_table_name, appreg_action, appreg_key, csds_json, error
                        FROM %s.csds_audit
                        ORDER BY ca_id
                        """
                                .formatted(schema),
                        new MapSqlParameterSource());

        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst())
                .containsEntry("appreg_table_name", "APPLICATION_CODES")
                .containsEntry("appreg_action", "INSERT")
                .containsEntry("error", null);
        assertThat(((Number) audits.getFirst().get("appreg_key")).longValue())
                .isEqualTo(insertedId + 100000L);
        assertThat((String) audits.getFirst().get("csds_json"))
                .contains("\"ApplicationCodeID\":%d".formatted(insertedId));
    }

    @Test
    void given_errorAuditLevel_when_batchUpsertFails_then_persistsOnlyFailingRowAudit() {
        setAuditLevel("ERROR");

        var insertedId =
                applicationCodeRepository.findAll().stream()
                                .map(ApplicationCode::getId)
                                .max(Long::compareTo)
                                .orElseThrow()
                        + 2000;
        var validInsertedRecord = createInsertedRecord(insertedId);
        var failingInsertedRecord = createInsertedRecord(insertedId + 1);
        failingInsertedRecord.put("ApplicationTitle", "X".repeat(501));

        assertThatThrownBy(
                        () ->
                                applicationCodeDataIngressProcessor.ingest(
                                        List.of(page(validInsertedRecord, failingInsertedRecord))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CSDS batch upsert failed");

        var audits =
                jdbcTemplate.queryForList(
                        """
                        SELECT appreg_table_name, appreg_action, appreg_key, csds_json, error
                        FROM %s.csds_audit
                        ORDER BY ca_id
                        """
                                .formatted(schema),
                        new MapSqlParameterSource());

        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst())
                .containsEntry("appreg_table_name", "APPLICATION_CODES")
                .containsEntry("appreg_action", "INSERT");
        assertThat(((Number) audits.getFirst().get("appreg_key")).longValue())
                .isEqualTo(insertedId + 100001L);
        assertThat((String) audits.getFirst().get("csds_json"))
                .contains("\"ApplicationCodeID\":%d".formatted(insertedId + 1));
        assertThat((String) audits.getFirst().get("error")).containsIgnoringCase("value too long");
        assertThat(applicationCodeRepository.findById(insertedId + 100000L)).isEmpty();
        assertThat(applicationCodeRepository.findById(insertedId + 100001L)).isEmpty();
    }

    private ObjectNode toSourceRecordWithPssacid(
            ApplicationCode applicationCode, Long applicationCodeId) {
        var node =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("ApplicationCodeID", applicationCodeId)
                        .put("PSSApplicationCodeID", applicationCode.getId())
                        .put("Code", applicationCode.getCode())
                        .put("ApplicationTitle", applicationCode.getTitle())
                        .put("ApplicationWording", applicationCode.getWording())
                        .put("FeeDue", applicationCode.getFeeDue().getValue())
                        .put("Respondent", applicationCode.getRequiresRespondent().getValue())
                        .put("StartDate", applicationCode.getStartDate().toString())
                        .putNull("Notes")
                        .put(
                                "BulkRespondentAllowed",
                                applicationCode.getBulkRespondentAllowed().getValue())
                        .put("AuthoringStatus", "Published")
                        .put("PublishingStatus", "Active")
                        .put("CurrentRecordIndicator", true)
                        .put("DraftFinalExistsIndicator", false)
                        .put("RevisionNumber", applicationCode.getVersion())
                        .put("RevisionType", "Initial")
                        .putNull("RevisionDateFrom")
                        .putNull("RevisionDateTo")
                        .putNull("ClonedFrom")
                        .putNull("PSSChangeSetHeaderID")
                        .putNull("PSSChangeSetItemID")
                        .put("FID_ApplicationRegisterHeader", 11263L)
                        .putNull("FID_ReleasePackage")
                        .put("Updator", "migration");

        putNullableText(node, "Legislation", applicationCode.getLegislation());
        putNullableText(node, "FeeReference", applicationCode.getFeeReference());
        putNullableDate(node, "EndDate", applicationCode.getEndDate());

        return node;
    }

    private ObjectNode createInsertedRecord(Long insertedId) {
        return OBJECT_MAPPER
                .createObjectNode()
                .put("ApplicationCodeID", insertedId)
                .put("Code", "AA99999")
                .put("ApplicationTitle", "Inserted Title")
                .put("ApplicationWording", "Inserted Wording")
                .putNull("Legislation")
                .put("FeeDue", YesOrNo.YES.getValue())
                .put("FeeReference", "FEE-1")
                .put("Respondent", YesOrNo.NO.getValue())
                .put("StartDate", "2020-01-01")
                .putNull("EndDate")
                .putNull("Notes")
                .put("BulkRespondentAllowed", YesOrNo.NO.getValue())
                .put("AuthoringStatus", "Published")
                .put("PublishingStatus", "Active")
                .put("CurrentRecordIndicator", true)
                .put("DraftFinalExistsIndicator", false)
                .put("RevisionNumber", 1)
                .put("RevisionType", "Initial")
                .putNull("RevisionDateFrom")
                .putNull("RevisionDateTo")
                .putNull("ClonedFrom")
                .putNull("PSSApplicationCodeID")
                .putNull("PSSChangeSetHeaderID")
                .putNull("PSSChangeSetItemID")
                .put("FID_ApplicationRegisterHeader", 11263L)
                .putNull("FID_ReleasePackage")
                .put("Updator", "migration");
    }

    private ObjectNode page(ObjectNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var array = page.putArray("records");
        for (var record : records) {
            array.add(record);
        }
        return page;
    }

    private void setAuditLevel(String level) {
        jdbcTemplate.update(
                """
                UPDATE %s.configuration_parameters
                SET parameter_value = :parameterValue
                WHERE parameter_name = 'AUDIT_CSDS'
                """
                        .formatted(schema),
                new MapSqlParameterSource("parameterValue", level));
    }

    private void putNullableDate(ObjectNode node, String fieldName, LocalDate value) {
        if (value == null) {
            node.putNull(fieldName);
            return;
        }

        node.put(fieldName, value.toString());
    }

    private void putNullableText(ObjectNode node, String fieldName, String value) {
        if (value == null) {
            node.putNull(fieldName);
            return;
        }

        node.put(fieldName, value);
    }
}
