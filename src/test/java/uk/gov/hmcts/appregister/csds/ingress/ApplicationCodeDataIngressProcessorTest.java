package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
class ApplicationCodeDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private ApplicationCodeRepository applicationCodeRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private ApplicationCodeDiffReportingService diffReportingService;
    private ApplicationCodeDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        diffReportingService =
                new ApplicationCodeDiffReportingService(
                        properties, applicationCodeRepository, applicationListEntryRepository);
        processor = new ApplicationCodeDataIngressProcessor(properties, diffReportingService);
    }

    @Test
    void given_countExceedsPageSize_when_retrieve_then_pagesThroughQueryEndpoint() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode().putArray("records");
        var secondPage = OBJECT_MAPPER.createObjectNode().putArray("records");

        when(ingressClient.retrieveJson("/count/CSDS/ApplicationCode/GD"))
                .thenReturn(countResponse);
        when(ingressClient.retrieveJson("/query/CSDS/ApplicationCode/GD?%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson("/query/CSDS/ApplicationCode/GD?%24limit=2&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_processedData_when_handle_then_logsDiffAgainstExistingApplicationCodes() {
        var existingUnchanged = createApplicationCode(1L, "A1", "Title 1", "Wording 1", 1L);
        var existingUpdated = createApplicationCode(2L, "A2", "Title 2", "Wording 2", 1L);
        var existingDeleted = createApplicationCode(4L, "A4", "Title 4", "Wording 4", 1L);
        when(applicationCodeRepository.findAll())
                .thenReturn(List.of(existingUnchanged, existingUpdated, existingDeleted));
        when(applicationListEntryRepository.countByApplicationCodeIds(List.of(4L)))
                .thenReturn(List.of(newReferenceCount(4L, 2L)));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecord(1L, "A1", "Title 1", "Wording 1", 1L),
                                createSourceRecord(2L, "A2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L)));

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.handle(processedData);

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "incoming=3, existing=3, inserts=1, updates=1, deletes=1, unchanged=1"))
                .anyMatch(log -> log.contains("CSDS insert preview") && log.contains("3"))
                .anyMatch(log -> log.contains("CSDS delete preview") && log.contains("4"))
                .anyMatch(
                        log ->
                                log.contains("CSDS protected delete preview")
                                        && log.contains("4 references=2"))
                .anyMatch(
                        log ->
                                log.contains("CSDS update preview")
                                        && log.contains("2 changed fields [title, version]"));
    }

    @Test
    void given_comparisonOutputDirConfigured_when_handle_then_writesCsvFiles() throws Exception {
        properties.getProcessors().getApplicationCodes().setComparisonOutputDir(tempDir.toString());
        diffReportingService =
                new ApplicationCodeDiffReportingService(
                        properties, applicationCodeRepository, applicationListEntryRepository);
        processor = new ApplicationCodeDataIngressProcessor(properties, diffReportingService);

        var existingUpdated = createApplicationCode(1L, "A1", "Title 1", "Wording 1", 1L);
        var existingDeleted = createApplicationCode(4L, "A4", "Title 4", "Wording 4", 1L);
        when(applicationCodeRepository.findAll())
                .thenReturn(List.of(existingUpdated, existingDeleted));
        when(applicationListEntryRepository.countByApplicationCodeIds(List.of(4L)))
                .thenReturn(List.of(newReferenceCount(4L, 3L)));

        processor.handle(
                List.of(
                        createPageResponseWithMetadata(
                                99,
                                createSourceRecord(1L, "A1", "Title 1 Duplicate", "Wording 1", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L))));

        try (var fileStream = Files.list(tempDir)) {
            var createdFiles = fileStream.toList();

            assertThat(createdFiles)
                    .extracting(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith("application_codes_incoming_raw_"))
                    .anyMatch(name -> name.startsWith("application_codes_incoming_"))
                    .anyMatch(name -> name.startsWith("application_codes_existing_"))
                    .anyMatch(name -> name.startsWith("application_codes_diff_"));

            var rawIncomingCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .startsWith(
                                                                    "application_codes_incoming_raw_"))
                                    .findFirst()
                                    .orElseThrow());
            var existingCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .startsWith(
                                                                    "application_codes_existing_"))
                                    .findFirst()
                                    .orElseThrow());
            var diffCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .startsWith("application_codes_diff_"))
                                    .findFirst()
                                    .orElseThrow());

            assertThat(rawIncomingCsv)
                    .contains("page_responseCode")
                    .contains("rawRecordJson")
                    .contains("\"0\",\"0\",\"99\"")
                    .contains("\"0\",\"1\",\"99\"")
                    .contains("\"1\",\"A1\",\"Title 1 Duplicate\"");
            assertThat(existingCsv)
                    .contains("referenceCount")
                    .contains("\"4\",\"A4\"")
                    .contains("\"3\"");
            assertThat(diffCsv)
                    .contains("id,changeType,changedFields,referencedByRi")
                    .contains("\"1\",\"update\",\"title, version\",")
                    .contains("\"3\",\"insert\",\"\",")
                    .contains("\"4\",\"delete\",\"\",\"true\"");
        }
    }

    @Test
    void given_queryResponseMissingRecordsArray_when_handle_then_throwException() {
        var invalidPage = OBJECT_MAPPER.createObjectNode().put("unexpected", true);

        assertThatThrownBy(() -> processor.handle(List.of(invalidPage)))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("records array");
    }

    @Test
    void given_processedData_when_preProcess_then_returnsSamePayload() {
        List<JsonNode> processedData =
                List.of(OBJECT_MAPPER.createObjectNode().putArray("records"));

        assertThat(processor.preProcess(processedData)).containsExactlyElementsOf(processedData);
    }

    @Test
    void given_countEndpoint_when_retrieve_then_callsItFirst() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 0);
        when(ingressClient.retrieveJson("/count/CSDS/ApplicationCode/GD"))
                .thenReturn(countResponse);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).isEmpty();
        verify(ingressClient).retrieveJson("/count/CSDS/ApplicationCode/GD");
        verify(applicationListEntryRepository, never()).countByApplicationCodeIds(anyCollection());
    }

    private ApplicationListEntryRepository.ApplicationCodeReferenceCount newReferenceCount(
            Long applicationCodeId, Long referenceCount) {
        return new ApplicationListEntryRepository.ApplicationCodeReferenceCount() {
            @Override
            public Long getApplicationCodeId() {
                return applicationCodeId;
            }

            @Override
            public long getReferenceCount() {
                return referenceCount;
            }
        };
    }

    private ApplicationCode createApplicationCode(
            Long id, String code, String title, String wording, Long version) {
        return ApplicationCode.builder()
                .id(id)
                .code(code)
                .title(title)
                .wording(wording)
                .legislation(null)
                .feeDue(YesOrNo.YES)
                .requiresRespondent(YesOrNo.NO)
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(null)
                .bulkRespondentAllowed(YesOrNo.NO)
                .version(version)
                .feeReference("FEE-1")
                .build();
    }

    private com.fasterxml.jackson.databind.node.ObjectNode createPageResponse(
            com.fasterxml.jackson.databind.node.ObjectNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var recordsArray = page.putArray("records");
        for (var record : records) {
            recordsArray.add(record);
        }
        return page;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode createPageResponseWithMetadata(
            int responseCode, com.fasterxml.jackson.databind.node.ObjectNode... records) {
        var page = createPageResponse(records);
        page.put("responseCode", responseCode);
        return page;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode createSourceRecord(
            Long id, String code, String title, String wording, Long version) {
        return OBJECT_MAPPER
                .createObjectNode()
                .put("ApplicationCodeID", id)
                .put("Code", code)
                .put("ApplicationTitle", title)
                .put("ApplicationWording", wording)
                .putNull("Legislation")
                .put("FeeDue", "Y")
                .put("Respondent", "N")
                .put("StartDate", "2020-01-01")
                .putNull("EndDate")
                .put("BulkRespondentAllowed", "N")
                .put("VersionNumber", version)
                .put("FeeReference", "FEE-1");
    }
}
