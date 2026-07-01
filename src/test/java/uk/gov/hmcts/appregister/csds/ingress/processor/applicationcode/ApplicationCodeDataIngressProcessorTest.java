package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;

@ExtendWith(MockitoExtension.class)
class ApplicationCodeDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private ApplicationCodeRepository applicationCodeRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private ApplicationCodeDiffService diffService;
    private ApplicationCodeDiffReportingService diffReportingService;
    private ApplicationCodeDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        properties.getProcessors().getApplicationCodes().setReportingDir(tempDir.toString());
        diffService = new ApplicationCodeDiffService(applicationCodeRepository);
        diffReportingService =
                new ApplicationCodeDiffReportingService(properties, applicationListEntryRepository);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties, diffService, diffReportingService);
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
    void given_processorParametersConfigured_when_retrieve_then_appendsThemBeforePaging() {
        properties
                .getProcessors()
                .getApplicationCodes()
                .setParameters("?$f=PublishingStatus='Active'&$expr=Updator");

        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode().putArray("records");
        var secondPage = OBJECT_MAPPER.createObjectNode().putArray("records");
        var parameterisedQueryPath =
                "/query/CSDS/ApplicationCode/GD?$f=PublishingStatus='Active'&$expr=Updator";

        when(ingressClient.retrieveJson("/count/CSDS/ApplicationCode/GD"))
                .thenReturn(countResponse);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=2&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_mockFileConfigured_when_retrieve_then_loadsMockResponseInsteadOfCallingClient() {
        properties.getProcessors().getApplicationCodes().setMock("csds/application_codes.json");

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.getFirst().get("records")).isNotNull();
        verifyNoInteractions(ingressClient);
    }

    @Test
    void given_processedData_when_apply_then_logsDiffAgainstExistingApplicationCodes() {
        var existingIgnored =
                createApplicationCode(
                        1L,
                        "A1",
                        "Title 1",
                        "Wording 1",
                        1L,
                        LocalDate.of(2020, 1, 1),
                        LocalDate.now().minusDays(1));
        var existingUpdated =
                createApplicationCode(
                        2L, "A2", "Title 2", "Wording 2", 1L, LocalDate.of(2020, 1, 1), null);
        var existingUnmatched =
                createApplicationCode(
                        4L, "A4", "Title 4", "Wording 4", 1L, LocalDate.of(2020, 1, 1), null);
        when(applicationCodeRepository.findAll())
                .thenReturn(List.of(existingIgnored, existingUpdated, existingUnmatched));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecord(
                                        1L,
                                        "A1",
                                        "Title 1",
                                        "Wording 1",
                                        1L,
                                        "2020-01-01",
                                        LocalDate.now().minusDays(1).toString()),
                                createSourceRecord(2L, "A2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L)));

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(processedData);

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "incoming=3, existing=3, inserts=1, updates=1, ignores=1"))
                .anyMatch(log -> log.contains("CSDS insert preview") && log.contains("100003"))
                .anyMatch(log -> log.contains("CSDS update preview") && log.contains("100002"))
                .anyMatch(log -> log.contains("CSDS ignore preview") && log.contains("100001"));
    }

    @Test
    void given_reportingDirConfigured_when_apply_then_writesCsvFiles() throws Exception {
        properties.getProcessors().getApplicationCodes().setReportingDir(tempDir.toString());
        diffService = new ApplicationCodeDiffService(applicationCodeRepository);
        diffReportingService =
                new ApplicationCodeDiffReportingService(properties, applicationListEntryRepository);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties, diffService, diffReportingService);

        var existingUpdated =
                createApplicationCode(
                        1L, "A1", "Title 1", "Wording 1", 1L, LocalDate.of(2020, 1, 1), null);
        var existingIgnored =
                createApplicationCode(
                        4L,
                        "A4",
                        "Title 4",
                        "Wording 4",
                        1L,
                        LocalDate.of(2020, 1, 1),
                        LocalDate.now().minusDays(1));
        when(applicationCodeRepository.findAll())
                .thenReturn(List.of(existingUpdated, existingIgnored));
        when(applicationListEntryRepository.countByApplicationCodeIds(List.of(1L, 4L)))
                .thenReturn(List.of(newReferenceCount(4L, 3L)));

        processor.apply(
                List.of(
                        createPageResponseWithMetadata(
                                99,
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L),
                                createSourceRecord(1L, "A1", "Title 1 Duplicate", "Wording 1", 2L),
                                createSourceRecord(
                                        4L,
                                        "A4",
                                        "Title 4",
                                        "Wording 4",
                                        1L,
                                        "2020-01-01",
                                        LocalDate.now().minusDays(1).toString()))));

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
                    .contains("\"0\",\"0\",\"99\"")
                    .contains("\"0\",\"1\",\"99\"")
                    .contains("\"1\",\"A1\",\"Title 1 Duplicate\"");
            assertThat(existingCsv)
                    .contains("referenceCount")
                    .contains("\"4\",\"A4\"")
                    .contains("\"3\"");
            assertThat(diffCsv)
                    .contains("id,changeType")
                    .contains("\"100001\",\"update\"")
                    .contains("\"100003\",\"insert\"")
                    .contains("\"100004\",\"ignore\"");
            assertThat(rawIncomingCsv.indexOf("\"1\",\"A1\",\"Title 1 Duplicate\""))
                    .isLessThan(rawIncomingCsv.indexOf("\"3\",\"A3\",\"Title 3\""));
            assertThat(rawIncomingCsv.indexOf("\"3\",\"A3\",\"Title 3\""))
                    .isLessThan(rawIncomingCsv.indexOf("\"4\",\"A4\",\"Title 4\""));
        }
    }

    @Test
    void given_pssacidPresent_when_apply_then_useItAsTheResolvedKey() {
        when(applicationCodeRepository.findAll()).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                List.of(
                        createPageResponse(
                                createSourceRecordWithoutApplicationCodeId(
                                        345L, "A3", "Title 3", "Wording 3", 1L))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains("CSDS insert preview")
                                        && log.contains(String.valueOf(345L)));
    }

    @Test
    void given_pssacidMissing_when_apply_then_useApplicationCodeIdOffsetKey() {
        when(applicationCodeRepository.findAll()).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                List.of(
                        createPageResponse(
                                createSourceRecord(345L, "A3", "Title 3", "Wording 3", 1L))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains("CSDS insert preview")
                                        && log.contains(String.valueOf(100345L)));
    }

    @Test
    void given_queryResponseMissingRecordsArray_when_apply_then_throwException() {
        var invalidPage = OBJECT_MAPPER.createObjectNode().put("unexpected", true);

        assertThatThrownBy(() -> processor.apply(List.of(invalidPage)))
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

    @Test
    void given_reportingDirMissing_when_apply_then_skipDiffReportingWork() {
        properties.getProcessors().getApplicationCodes().setReportingDir(null);
        diffService = new ApplicationCodeDiffService(applicationCodeRepository);
        diffReportingService =
                new ApplicationCodeDiffReportingService(properties, applicationListEntryRepository);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties, diffService, diffReportingService);
        when(applicationCodeRepository.findAll()).thenReturn(List.of());

        processor.apply(
                List.of(
                        createPageResponse(
                                createSourceRecord(1L, "A1", "Title 1", "Wording 1", 1L))));

        verify(applicationCodeRepository).findAll();
        verifyNoInteractions(applicationListEntryRepository);
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
            Long id,
            String code,
            String title,
            String wording,
            Long version,
            LocalDate startDate,
            LocalDate endDate) {
        return ApplicationCode.builder()
                .id(id)
                .code(code)
                .title(title)
                .wording(wording)
                .legislation(null)
                .feeDue(YesOrNo.YES)
                .requiresRespondent(YesOrNo.NO)
                .startDate(startDate)
                .endDate(endDate)
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
        return createSourceRecord(id, code, title, wording, version, "2020-01-01", null);
    }

    private com.fasterxml.jackson.databind.node.ObjectNode createSourceRecord(
            Long id,
            String code,
            String title,
            String wording,
            Long version,
            String startDate,
            String endDate) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("ApplicationCodeID", id)
                        .put("Code", code)
                        .put("ApplicationTitle", title)
                        .put("ApplicationWording", wording)
                        .putNull("Legislation")
                        .put("FeeDue", "Y")
                        .put("Respondent", "N")
                        .put("StartDate", startDate)
                        .put("BulkRespondentAllowed", "N")
                        .put("VersionNumber", version)
                        .put("FeeReference", "FEE-1");
        if (endDate == null) {
            record.putNull("EndDate");
            return record;
        }

        record.put("EndDate", endDate);
        return record;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode
            createSourceRecordWithoutApplicationCodeId(
                    Long pssacid, String code, String title, String wording, Long version) {
        var record = createSourceRecord(0L, code, title, wording, version, "2020-01-01", null);
        record.remove("ApplicationCodeID");
        record.put("PSSACID", pssacid);
        return record;
    }
}
