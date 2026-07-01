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
            "appreg.csds.ingress.enabled=true",
            "appreg.csds.ingress.page-size=2",
            "appreg.csds.ingress.processors.application-codes.enabled=true",
            "appreg.csds.ingress.processors.application-codes.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.application-codes.mock=",
            "appreg.csds.ingress.processors.application-codes.parameters=",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}",
            "appreg.csds.ingress.access-keys[0]=primary-test-key",
            "appreg.csds.ingress.access-keys[1]=secondary-test-key"
        })
class ApplicationCodeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private CsdsIngressProcessor csdsIngressProcessor;
    @Autowired private DatabaseJobRepository databaseJobRepository;
    @Autowired private ApplicationCodeRepository applicationCodeRepository;

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
    }

    @Test
    void given_applicationCodeProcessorEnabled_when_runIngress_then_logsDiffSummary()
            throws JsonProcessingException {
        var existingApplicationCodes =
                applicationCodeRepository.findAll().stream()
                        .sorted(Comparator.comparing(ApplicationCode::getId))
                        .toList();
        var ignored = existingApplicationCodes.get(0);
        var updated = existingApplicationCodes.get(1);
        var unmatched = existingApplicationCodes.get(2);

        ignored.setEndDate(LocalDate.now().minusDays(1));
        updated.setEndDate(null);
        unmatched.setEndDate(null);
        applicationCodeRepository.saveAll(List.of(ignored, updated, unmatched));

        var incomingRecords =
                new ArrayList<>(
                        existingApplicationCodes.stream().map(this::toSourceRecord).toList());
        incomingRecords.removeIf(
                record -> record.get("ApplicationCodeID").longValue() == unmatched.getId());
        incomingRecords.get(1).put("ApplicationTitle", updated.getTitle() + " (Updated)");
        incomingRecords.get(1).put("VersionNumber", updated.getVersion() + 1);

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
        var expectedUpdates = totalCount - 2;
        var expectedInsertedId = insertedId + 100000;
        var expectedUpdatedId = updated.getId() + 100000;
        var expectedIgnoredId = ignored.getId() + 100000;

        assertThat(executed).isTrue();
        assertThat(databaseJob.getMetadata()).isNull();
        assertThat(databaseJob.getLastRan()).isNull();
        assertThat(pagedProcessorLogs.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved " + expectedPageCount + " CSDS pages"));
        assertThat(applicationCodeDiffReportingLogs.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "incoming=%d, existing=%d, inserts=1, updates=%d, ignores=1"
                                                .formatted(
                                                        totalCount,
                                                        existingApplicationCodes.size(),
                                                        expectedUpdates)))
                .anyMatch(
                        log ->
                                log.contains("CSDS insert preview")
                                        && log.contains(String.valueOf(expectedInsertedId)))
                .anyMatch(
                        log ->
                                log.contains("CSDS update preview")
                                        && log.contains(String.valueOf(expectedUpdatedId)))
                .anyMatch(
                        log ->
                                log.contains("CSDS ignore preview")
                                        && log.contains(String.valueOf(expectedIgnoredId)));

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

    private ObjectNode toSourceRecord(ApplicationCode applicationCode) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("ApplicationCodeID", applicationCode.getId())
                        .put("Code", applicationCode.getCode())
                        .put("ApplicationTitle", applicationCode.getTitle())
                        .put("ApplicationWording", applicationCode.getWording())
                        .put("FeeDue", applicationCode.getFeeDue().getValue())
                        .put("Respondent", applicationCode.getRequiresRespondent().getValue())
                        .put("VersionNumber", applicationCode.getVersion())
                        .put("StartDate", applicationCode.getStartDate().toString())
                        .put(
                                "BulkRespondentAllowed",
                                applicationCode.getBulkRespondentAllowed().getValue());

        putNullableText(record, "Legislation", applicationCode.getLegislation());
        putNullableText(record, "FeeReference", applicationCode.getFeeReference());
        putNullableDate(record, "EndDate", applicationCode.getEndDate());

        return record;
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
                .put("VersionNumber", 1)
                .put("StartDate", "2020-01-01")
                .putNull("EndDate")
                .put("BulkRespondentAllowed", YesOrNo.NO.getValue());
    }

    private void putNullableDate(ObjectNode record, String fieldName, java.time.LocalDate value) {
        if (value == null) {
            record.putNull(fieldName);
            return;
        }

        record.put(fieldName, value.toString());
    }

    private void putNullableText(ObjectNode record, String fieldName, String value) {
        if (value == null) {
            record.putNull(fieldName);
            return;
        }

        record.put(fieldName, value);
    }
}
