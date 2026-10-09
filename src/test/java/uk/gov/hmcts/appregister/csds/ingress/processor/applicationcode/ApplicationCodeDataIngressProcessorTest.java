package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService.BackupResult;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsPayloadValidationException;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class ApplicationCodeDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private ApplicationCodeDiffService diffService;
    private ApplicationCodeDiffReportingService diffReportingService;
    private ApplicationCodeIngressDatabaseRowMapper rowMapper;
    private ApplicationCodeDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        properties.getProcessors().getApplicationCodes().setReportingDir(tempDir.toString());
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        rowMapper = new ApplicationCodeIngressDatabaseRowMapper();
        diffService = new ApplicationCodeDiffService(tableReadService, rowMapper);
        diffReportingService = new ApplicationCodeDiffReportingService(properties);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        diffReportingService,
                        bulkUpsertService,
                        rowMapper,
                        Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneOffset.UTC));
    }

    @Test
    void given_newCodeWithoutSourceId_when_diff_then_reject() {
        var records =
                List.<JsonNode>of(
                        createPageResponse(
                                createSourceRecordWithoutApplicationCodeId(
                                        345L, "A3", "Title", "Wording", 1L)));
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        assertThatThrownBy(() -> processor.diff(records))
                .hasMessageContaining("Missing ApplicationCodeID");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void given_distinctNewCodesWithSameSourceId_when_diff_then_rejectIdCollision() {
        var records =
                List.<JsonNode>of(
                        createPageResponse(
                                createSourceRecord(3L, "A3", "Title", "Wording", 1L),
                                createSourceRecord(3L, "A4", "Title", "Wording", 1L)));
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        assertThatThrownBy(() -> processor.diff(records))
                .hasMessageContaining("Conflicting AC_ID 100003");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void given_sourceIdOverflow_when_diff_then_reject() {
        var records =
                List.<JsonNode>of(
                        createPageResponse(
                                createSourceRecord(Long.MAX_VALUE, "A3", "Title", "Wording", 1L)));
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        assertThatThrownBy(() -> processor.diff(records)).isInstanceOf(ArithmeticException.class);
        verifyNoInteractions(bulkUpsertService);
    }

    private CsdsIngressTransactionRunner passthroughTransactionRunner() {
        return new CsdsIngressTransactionRunner() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> supplier) {
                return supplier.get();
            }
        };
    }

    @Test
    void given_fullResponse_when_retrieve_then_usesOnlyCountAndFetch() throws Exception {
        var page = OBJECT_MAPPER.readTree("{\"records\":[{},{},{}]}");
        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(OBJECT_MAPPER.createObjectNode().put("count", 3));
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=3&%24offset=0"))
                .thenReturn(page);

        assertThat(processor.retrieve(ingressClient)).containsExactly(page);
        verify(ingressClient).retrieveJson("/count/APPREGISTER/ApplicationCode/GD");
        verify(ingressClient)
                .retrieveJson("/query/APPREGISTER/ApplicationCode/GD?%24limit=3&%24offset=0");
        verifyNoMoreInteractions(ingressClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"records\":[]}", "{\"records\":[{},{}]}"})
    void given_countMismatch_when_retrieve_then_rejectBeforeWrites(String response)
            throws Exception {
        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(OBJECT_MAPPER.createObjectNode().put("count", 1));
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=1&%24offset=0"))
                .thenReturn(OBJECT_MAPPER.readTree(response));

        assertThatThrownBy(() -> processor.retrieve(ingressClient))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("CSDS record count mismatch");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void given_emptyFallbackPage_when_retrieve_then_rejectPartialDataset() throws Exception {
        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(OBJECT_MAPPER.createObjectNode().put("count", 3));
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=3&%24offset=0"))
                .thenReturn(OBJECT_MAPPER.readTree("{\"records\":[{}]}"));
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=2&%24offset=1"))
                .thenReturn(OBJECT_MAPPER.readTree("{\"records\":[]}"));

        assertThatThrownBy(() -> processor.retrieve(ingressClient))
                .hasMessageContaining("at offset 1");
        verifyNoInteractions(bulkUpsertService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.5", "2147483648", "null", "\"3\""})
    void given_invalidCount_when_retrieve_then_reject(String count) throws Exception {
        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(OBJECT_MAPPER.readTree("{\"count\":" + count + "}"));
        assertThatThrownBy(() -> processor.retrieve(ingressClient))
                .isInstanceOf(AppRegistryException.class);
        verify(ingressClient).retrieveJson("/count/APPREGISTER/ApplicationCode/GD");
        verifyNoMoreInteractions(ingressClient);
    }

    @Test
    void given_serverCapsResponse_when_retrieve_then_advancesByReceivedRecords() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records").addObject();
        var secondPage = OBJECT_MAPPER.createObjectNode();
        secondPage.putArray("records").addObject();
        secondPage.withArray("records").addObject();

        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(countResponse);
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=2&%24offset=1"))
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
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records").addObject();
        firstPage.withArray("records").addObject();
        var secondPage = OBJECT_MAPPER.createObjectNode();
        secondPage.putArray("records").addObject();
        var parameterisedCountPath =
                "/count/APPREGISTER/ApplicationCode/GD?$f=PublishingStatus='Active'&$expr=Updator";
        var parameterisedQueryPath =
                "/query/APPREGISTER/ApplicationCode/GD?$f=PublishingStatus='Active'&$expr=Updator";

        when(ingressClient.retrieveJson(parameterisedCountPath)).thenReturn(countResponse);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=1&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_mockFileConfigured_when_retrieve_then_loadsMockResponseInsteadOfCallingClient() {
        properties.getProcessors().getApplicationCodes().setMock("csds/application_codes.json");
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.getFirst().get("records")).isNotNull();
        verifyNoInteractions(ingressClient);
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Loaded CSDS mock response for application_codes "
                                                + "from classpath:csds/application_codes.json"))
                .anyMatch(
                        log ->
                                log.contains(
                                        "Loaded mock CSDS payload for application_codes "
                                                + "with 200 records"));
    }

    @Test
    void given_missingMockFileConfigured_when_retrieve_then_logsAndFallsBackToEndpoint() {
        properties
                .getProcessors()
                .getApplicationCodes()
                .setMock("classpath:csds/does_not_exist.json");
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 1);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records").addObject();

        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(countResponse);
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=1&%24offset=0"))
                .thenReturn(firstPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage);
        assertThat(logCaptor.getWarnLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Configured CSDS mock response for application_codes "
                                                + "was not found at "
                                                + "classpath:csds/does_not_exist.json. "
                                                + "Falling back to endpoint."));
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved 1 CSDS pages for application_codes"));
    }

    @Test
    void given_invalidMockFileConfigured_when_retrieve_then_logsErrorAndFallsBackToEndpoint()
            throws Exception {
        var invalidMockFile = tempDir.resolve("invalid-application-codes.json");
        Files.writeString(invalidMockFile, "{ not-json");
        properties.getProcessors().getApplicationCodes().setMock(invalidMockFile.toString());
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 1);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records").addObject();

        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(countResponse);
        when(ingressClient.retrieveJson(
                        "/query/APPREGISTER/ApplicationCode/GD?%24limit=1&%24offset=0"))
                .thenReturn(firstPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage);
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to load CSDS mock response for application_codes from "
                                                + invalidMockFile
                                                + ". Falling back to endpoint."));
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved 1 CSDS pages for application_codes"));
    }

    @Test
    void given_processedData_when_apply_then_logsDiffAgainstExistingApplicationCodes() {
        var existingUpdated =
                createApplicationCode(
                        345L,
                        "A2",
                        "Title 2",
                        "Wording 2",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        var existingUnmatched =
                createApplicationCode(
                        4L,
                        "A4",
                        "Title 4",
                        "Wording 4",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(
                        List.of(
                                ApplicationCodeIngressRecord.fromEntity(existingUpdated),
                                ApplicationCodeIngressRecord.fromEntity(existingUnmatched)));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssacid(
                                        345L, 2L, "A2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L)));

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();
        var processorLogCaptor = LogCaptor.forClass(ApplicationCodeDataIngressProcessor.class);
        processorLogCaptor.clearLogs();

        processor.apply(processor.preProcess(processedData));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=2, existing=2, inserts=1, updates=1"));
        assertThat(processorLogCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "CSDS ingress processor application_codes produced inserts=1, updates=1"));
    }

    @Test
    void given_processedData_when_ingest_then_returnsAppliedSummary() {
        var existingUpdated =
                createApplicationCode(
                        345L,
                        "A2",
                        "Title 2",
                        "Wording 2",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        var existingUnmatched =
                createApplicationCode(
                        4L,
                        "A4",
                        "Title 4",
                        "Wording 4",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(
                        List.of(
                                ApplicationCodeIngressRecord.fromEntity(existingUpdated),
                                ApplicationCodeIngressRecord.fromEntity(existingUnmatched)));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssacid(
                                        345L, 2L, "A2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L)));

        var response = processor.ingest(processedData);

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isEqualTo(1);
    }

    @Test
    void given_backupConfigured_when_backup_then_copySourceIntoTarget() {
        properties.getProcessors().getApplicationCodes().setBackupSource("application_codes");
        properties
                .getProcessors()
                .getApplicationCodes()
                .setBackupTarget("application_codes_staging");
        when(ingressBackupService.backup("application_codes", "application_codes_staging"))
                .thenReturn(new BackupResult(2, 3));
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();

        processor.backup();

        verify(ingressBackupService).backup("application_codes", "application_codes_staging");
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains("Completed CSDS backup for application_codes")
                                        && log.contains("deleted=2")
                                        && log.contains("inserted=3"));
    }

    @Test
    void given_backupFails_when_backup_then_logAndContinue() {
        properties.getProcessors().getApplicationCodes().setBackupSource("application_codes");
        properties
                .getProcessors()
                .getApplicationCodes()
                .setBackupTarget("application_codes_staging");
        doThrow(new IllegalStateException("backup boom"))
                .when(ingressBackupService)
                .backup("application_codes", "application_codes_staging");
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();

        processor.backup();

        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains("Failed CSDS backup for application_codes")
                                        && log.contains("Continuing ingress"));
    }

    @Test
    void given_reportingDirConfigured_when_apply_then_writesCsvFiles() throws Exception {
        properties.getProcessors().getApplicationCodes().setReportingDir(tempDir.toString());
        diffService = new ApplicationCodeDiffService(tableReadService, rowMapper);
        diffReportingService = new ApplicationCodeDiffReportingService(properties);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        diffReportingService,
                        bulkUpsertService,
                        rowMapper,
                        Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneOffset.UTC));

        var existingUpdated =
                createApplicationCode(
                        1L,
                        "A1",
                        "Title 1",
                        "Wording 1",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        var existingMatchedByPssacid =
                createApplicationCode(
                        4L,
                        "A4",
                        "Title 4",
                        "Wording 4",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        LocalDate.now().minusDays(1));
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(
                        List.of(
                                ApplicationCodeIngressRecord.fromEntity(existingUpdated),
                                ApplicationCodeIngressRecord.fromEntity(existingMatchedByPssacid)));

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponseWithMetadata(
                                        99,
                                        createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L),
                                        createSourceRecordWithPssacid(
                                                1L,
                                                999L,
                                                "A1",
                                                "Title 1 Duplicate",
                                                "Wording 1",
                                                2L),
                                        createSourceRecordWithPssacid(
                                                4L,
                                                "A4",
                                                "Title 4",
                                                "Wording 4",
                                                1L,
                                                "2020-01-01",
                                                LocalDate.now().minusDays(1).toString())))));

        try (var fileStream = Files.list(tempDir)) {
            var createdFiles = fileStream.toList();

            assertThat(createdFiles)
                    .extracting(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith("application_codes_incoming_"))
                    .anyMatch(
                            name ->
                                    name.startsWith("application_codes_incoming_")
                                            && name.endsWith(".csv"))
                    .anyMatch(name -> name.startsWith("application_codes_existing_"))
                    .anyMatch(name -> name.startsWith("application_codes_diff_"));

            var incomingJson =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                                    .toString()
                                                                    .startsWith(
                                                                            "application_codes_incoming_")
                                                            && path.getFileName()
                                                                    .toString()
                                                                    .endsWith(".json"))
                                    .findFirst()
                                    .orElseThrow());
            var incomingCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                                    .toString()
                                                                    .startsWith(
                                                                            "application_codes_incoming_")
                                                            && path.getFileName()
                                                                    .toString()
                                                                    .endsWith(".csv"))
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

            assertThat(incomingJson)
                    .contains("\"responseCode\" : 99")
                    .contains("\"records\"")
                    .contains("\"ApplicationCodeID\" : 999")
                    .contains("\"PSSApplicationCodeID\" : 1")
                    .contains("\"ApplicationTitle\" : \"Title 1 Duplicate\"");
            assertThat(incomingCsv)
                    .contains(
                            "pssApplicationCodeId,applicationCodeId,acId,code,title,wording,legislation")
                    .contains("\"1\",\"999\",,\"A1\",\"Title 1 Duplicate\"")
                    .contains(",\"3\",,\"A3\",\"Title 3\"");
            assertThat(existingCsv)
                    .contains(
                            "pssApplicationCodeId,applicationCodeId,acId,code,title,wording,legislation")
                    .doesNotContain("referenceCount")
                    .contains(",,\"4\",\"A4\"");
            assertThat(diffCsv)
                    .contains("pssApplicationCodeId,applicationCodeId,acId,changeType")
                    .contains("\"1\",\"999\",\"1\",\"update\"")
                    .contains(",\"3\",\"%s\",\"insert\"".formatted(100003L))
                    .contains("\"4\",,\"4\",\"update\"");
        }
    }

    @Test
    void given_processedData_when_diffCalculated_then_includeIntendedUpsertRecord() {
        var existingUpdated =
                createApplicationCode(
                        345L,
                        "A2",
                        "Title 2",
                        "Wording 2",
                        1L,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null);
        when(tableReadService.loadAll("application_codes_staging", rowMapper))
                .thenReturn(List.of(ApplicationCodeIngressRecord.fromEntity(existingUpdated)));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssacid(
                                        345L, 2L, "A2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L)));

        var diffResult =
                diffService.diff(
                        new ApplicationCodeDiffRequest(
                                "application_codes_staging",
                                processedData,
                                this::toIngressRecord,
                                this::extractRecordsFromPage));

        assertThat(diffResult.diffRecords()).hasSize(2);
        assertThat(diffResult.diffRecords())
                .anySatisfy(
                        diffRecord -> {
                            assertThat(diffRecord.operation()).isEqualTo(IngressOperation.UPDATE);
                            assertThat(diffRecord.existing()).isNotNull();
                            assertThat(diffRecord.intended()).isEqualTo(diffRecord.incoming());
                            assertThat(diffRecord.intended().id()).isEqualTo(345L);
                        })
                .anySatisfy(
                        diffRecord -> {
                            assertThat(diffRecord.operation()).isEqualTo(IngressOperation.INSERT);
                            assertThat(diffRecord.existing()).isNull();
                            assertThat(diffRecord.intended()).isEqualTo(diffRecord.incoming());
                            assertThat(diffRecord.intended().id()).isEqualTo(100003L);
                        });
    }

    @Test
    void given_existingCodeWithoutSourceId_when_apply_then_preserveStoredId() {
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(
                        List.of(
                                toIngressRecord(
                                        createSourceRecord(
                                                987L, "A3", "Old title", "Wording 3", 1L))));
        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecordWithoutApplicationCodeId(
                                                345L, "A3", "Title 3", "Wording 3", 1L)))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=1, inserts=0, updates=1"));
    }

    @Test
    void given_pssacidMissing_when_apply_then_useApplicationCodeIdOffsetKey() {
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(
                                                345L, "A3", "Title 3", "Wording 3", 1L)))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
    }

    @Test
    void given_revisionNumberPresent_when_apply_then_acceptLiveCsdsFieldName() {
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());
        var sourceRecord = createSourceRecord(345L, "A3", "Title 3", "Wording 3", 7L);

        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(processor.preProcess(List.of(createPageResponse(sourceRecord))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
    }

    @Test
    void given_nullStartDate_when_apply_then_includeOffendingRecordJsonInException() {
        var sourceRecord = createSourceRecord(345L, "A3", "Title 3", "Wording 3", 7L);
        sourceRecord.putNull("StartDate");

        assertThatThrownBy(
                        () ->
                                processor.apply(
                                        processor.preProcess(
                                                List.of(createPageResponse(sourceRecord)))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining(
                        "CSDS field StartDate was missing or invalid for application_codes")
                .hasMessageContaining("\"ApplicationCodeID\":345")
                .hasMessageContaining("\"Code\":\"A3\"");
    }

    @Test
    void given_missingRequiredText_when_apply_then_includeOffendingRecordJsonInException() {
        var sourceRecord = createSourceRecord(345L, "A3", "Title 3", "Wording 3", 7L);
        sourceRecord.putNull("ApplicationTitle");

        assertThatThrownBy(
                        () ->
                                processor.apply(
                                        processor.preProcess(
                                                List.of(createPageResponse(sourceRecord)))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining(
                        "CSDS field ApplicationTitle was missing or invalid for application_codes")
                .hasMessageContaining("\"ApplicationCodeID\":345")
                .hasMessageContaining("\"Code\":\"A3\"");
    }

    @Test
    void given_invalidYesOrNo_when_apply_then_includeOffendingRecordJsonInException() {
        var sourceRecord = createSourceRecord(345L, "A3", "Title 3", "Wording 3", 7L);
        sourceRecord.put("FeeDue", "MAYBE");

        assertThatThrownBy(
                        () ->
                                processor.apply(
                                        processor.preProcess(
                                                List.of(createPageResponse(sourceRecord)))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining(
                        "CSDS field FeeDue contained an unknown YesOrNo value for application_codes")
                .hasMessageContaining("\"ApplicationCodeID\":345")
                .hasMessageContaining("\"FeeDue\":\"MAYBE\"");
    }

    @Test
    void given_queryResponseMissingRecordsArray_when_apply_then_throwException() {
        var invalidPage = OBJECT_MAPPER.createObjectNode().put("unexpected", true);
        List<JsonNode> invalidPages = List.of(invalidPage);

        assertThatThrownBy(() -> processor.apply(invalidPages))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("records array");
    }

    @Test
    void given_firstRecordMissingRequiredField_when_apply_then_failBeforeDatabaseRead() {
        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecord(345L, "A3", "Title 3", "Wording 3", 1L),
                                createSourceRecord(346L, "A4", "Title 4", "Wording 4", 1L)));
        var invalidRecord =
                ((ObjectNode) extractRecordsFromPage(processedData.getFirst()).getFirst());
        invalidRecord.remove("ApplicationTitle");

        assertThatThrownBy(() -> processor.preProcess(processedData))
                .isInstanceOf(CsdsPayloadValidationException.class)
                .hasMessageContaining("ApplicationTitle");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_duplicateCodeAcrossPages_when_apply_then_throwException() {
        var logCaptor = LogCaptor.forClass(ApplicationCodeDiffService.class);
        logCaptor.clearLogs();

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecord(345L, "A3", "Title 3", "Wording 3", 1L)),
                        createPageResponse(
                                createSourceRecord(346L, "A3", "Title 4", "Wording 4", 2L)));
        var preProcessed = processor.preProcess(processedData);

        assertThatThrownBy(() -> processor.apply(preProcessed))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming application_code A3");
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                                "Duplicate incoming application_code A3 detected for application_codes")
                                        && log.contains("duplicate record"));
    }

    @Test
    void given_futureDuplicatesAcrossPages_when_preProcess_then_logAndDropBeforeDiff() {
        var past = createSourceRecord(1L, "A1", "Title", "Wording", 1L);
        past.put("StartDate", "2026-07-01");
        var today = past.deepCopy().put("ApplicationCodeID", 2L).put("Code", "A2");
        today.put("StartDate", "2026-07-02");
        var future = past.deepCopy().put("StartDate", "2026-07-03");
        try (var logs = LogCaptor.forClass(ApplicationCodeDataIngressProcessor.class)) {
            var processed =
                    processor.preProcess(
                            List.of(
                                    createPageResponse(future, past),
                                    createPageResponse(today, future)));
            assertThat(extractRecordsFromPage(processed.getFirst())).containsExactly(past, today);
            assertThat(logs.getWarnLogs())
                    .filteredOn(
                            log ->
                                    log.contains(
                                            "Dropping future-dated application code A1 with StartDate 2026-07-03"))
                    .hasSize(2);
            processor.apply(processed);
        }
    }

    @Test
    void given_onlyFutureRecords_when_preProcess_then_returnEmptyRecords() {
        var future = createSourceRecord(1L, "A1", "Title", "Wording", 1L);
        future.put("StartDate", "2026-07-03");
        assertThat(
                        extractRecordsFromPage(
                                processor
                                        .preProcess(List.of(createPageResponse(future)))
                                        .getFirst()))
                .isEmpty();
        assertThat(processor.preProcess(List.of())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\"invalid\"", "\"2026-02-30\"", "123"})
    void given_invalidStartDateOnLaterPage_when_preProcess_then_failBatch(String date)
            throws Exception {
        var valid = createSourceRecord(1L, "A1", "Title", "Wording", 1L);
        var invalid = valid.deepCopy();
        invalid.set("StartDate", OBJECT_MAPPER.readTree(date));
        assertThatThrownBy(
                        () ->
                                processor.preProcess(
                                        List.of(
                                                createPageResponse(valid),
                                                createPageResponse(invalid))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("StartDate");
        invalid.remove("StartDate");
        assertThatThrownBy(() -> processor.preProcess(List.of(createPageResponse(invalid))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("StartDate");
        verifyNoInteractions(bulkUpsertService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"invalid\"", "\"2026-02-30\"", "123"})
    void given_invalidEndDateOnFutureRecord_when_preProcess_then_failBatch(String date)
            throws Exception {
        var invalid = createSourceRecord(1L, "A1", "Title", "Wording", 1L);
        invalid.put("StartDate", "2026-07-03");
        invalid.set("EndDate", OBJECT_MAPPER.readTree(date));
        assertThatThrownBy(() -> processor.preProcess(List.of(createPageResponse(invalid))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("EndDate");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void given_processedData_when_preProcess_then_preserveIncomingOrder() {
        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L),
                                createSourceRecord(1L, "A1", "Title 1", "Wording 1", 1L),
                                createSourceRecord(2L, "A2", "Title 2", "Wording 2", 1L)));

        var preProcessed = processor.preProcess(processedData);

        assertThat(preProcessed).hasSize(1);
        assertThat(extractRecordsFromPage(preProcessed.getFirst()))
                .extracting(sourceRecord -> sourceRecord.get("ApplicationCodeID").longValue())
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    void given_unmappedCsdsMetadataAbsent_when_preProcess_then_preserveRecord() {
        var sourceRecord = createSourceRecord(3L, "A3", "Title 3", "Wording 3", 1L);
        sourceRecord.remove(
                List.of(
                        "Notes",
                        "AuthoringStatus",
                        "PublishingStatus",
                        "CurrentRecordIndicator",
                        "DraftFinalExistsIndicator",
                        "RevisionType",
                        "RevisionDateFrom",
                        "RevisionDateTo",
                        "ClonedFrom",
                        "PSSChangeSetHeaderID",
                        "PSSChangeSetItemID",
                        "FID_ApplicationRegisterHeader",
                        "FID_ReleasePackage",
                        "Updator"));

        var preProcessed = processor.preProcess(List.of(createPageResponse(sourceRecord)));

        assertThat(extractRecordsFromPage(preProcessed.getFirst()).getFirst().has("AC_ID"))
                .isFalse();
    }

    @Test
    void given_countEndpoint_when_retrieve_then_callsItFirst() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 0);
        when(ingressClient.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .thenReturn(countResponse);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).isEmpty();
        verify(ingressClient).retrieveJson("/count/APPREGISTER/ApplicationCode/GD");
    }

    @Test
    void given_reportingDirMissing_when_apply_then_skipDiffReportingWork() {
        properties.getProcessors().getApplicationCodes().setReportingDir(null);
        diffService = new ApplicationCodeDiffService(tableReadService, rowMapper);
        diffReportingService = new ApplicationCodeDiffReportingService(properties);
        processor =
                new ApplicationCodeDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        diffReportingService,
                        bulkUpsertService,
                        rowMapper,
                        Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneOffset.UTC));
        when(tableReadService.loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper))
                .thenReturn(List.of());

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(
                                                1L, "A1", "Title 1", "Wording 1", 1L)))));

        verify(tableReadService)
                .loadAll(
                        properties.getProcessors().getApplicationCodes().getIngressTarget(),
                        rowMapper);
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

    private ObjectNode createPageResponse(ObjectNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var recordsArray = page.putArray("records");
        for (var sourceRecord : records) {
            recordsArray.add(sourceRecord);
        }
        return page;
    }

    private ObjectNode createPageResponseWithMetadata(int responseCode, ObjectNode... records) {
        var page = createPageResponse(records);
        page.put("responseCode", responseCode);
        return page;
    }

    private ObjectNode createSourceRecord(
            Long id, String code, String title, String wording, Long version) {
        return createSourceRecord(id, code, title, wording, version, "2020-01-01", null);
    }

    private ObjectNode createSourceRecord(
            Long id,
            String code,
            String title,
            String wording,
            Long version,
            String startDate,
            String endDate) {
        var sourceRecord =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("Code", code)
                        .put("ApplicationTitle", title)
                        .put("ApplicationWording", wording)
                        .putNull("Legislation")
                        .put("FeeDue", "Y")
                        .putNull("FeeReference")
                        .put("Respondent", "N")
                        .put("StartDate", startDate)
                        .putNull("Notes")
                        .put("BulkRespondentAllowed", "N")
                        .put("AuthoringStatus", "Published")
                        .put("PublishingStatus", "Active")
                        .put("CurrentRecordIndicator", true)
                        .put("DraftFinalExistsIndicator", false)
                        .put("RevisionNumber", version)
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
        if (id == null) {
            sourceRecord.putNull("ApplicationCodeID");
        } else {
            sourceRecord.put("ApplicationCodeID", id);
        }
        if (endDate == null) {
            sourceRecord.putNull("EndDate");
            return sourceRecord;
        }

        sourceRecord.put("EndDate", endDate);
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithoutApplicationCodeId(
            Long pssacid, String code, String title, String wording, Long version) {
        var sourceRecord =
                createSourceRecord(null, code, title, wording, version, "2020-01-01", null);
        sourceRecord.put("PSSApplicationCodeID", pssacid);
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithPssacid(
            Long pssacid,
            Long applicationCodeId,
            String code,
            String title,
            String wording,
            Long version) {
        var sourceRecord = createSourceRecord(applicationCodeId, code, title, wording, version);
        sourceRecord.put("PSSApplicationCodeID", pssacid);
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithPssacid(
            Long pssacid,
            String code,
            String title,
            String wording,
            Long version,
            String startDate,
            String endDate) {
        var sourceRecord =
                createSourceRecord(null, code, title, wording, version, startDate, endDate);
        sourceRecord.put("PSSApplicationCodeID", pssacid);
        return sourceRecord;
    }

    private List<JsonNode> extractRecordsFromPage(JsonNode page) {
        var records = page.get("records");
        var extracted = new ArrayList<JsonNode>();
        records.forEach(extracted::add);
        return List.copyOf(extracted);
    }

    private ApplicationCodeIngressRecord toIngressRecord(JsonNode node) {
        return new ApplicationCodeIngressRecord(
                node.get("ApplicationCodeID").isNull()
                        ? null
                        : node.get("ApplicationCodeID").longValue(),
                node.get("Code").asText(),
                node.get("ApplicationTitle").asText(),
                node.get("ApplicationWording").asText(),
                node.get("Legislation").isNull() ? null : node.get("Legislation").asText(),
                YesOrNo.fromValue(node.get("FeeDue").asText()),
                YesOrNo.fromValue(node.get("Respondent").asText()),
                LocalDate.parse(node.get("StartDate").asText()),
                node.get("EndDate").isNull() ? null : LocalDate.parse(node.get("EndDate").asText()),
                YesOrNo.fromValue(node.get("BulkRespondentAllowed").asText()),
                node.get("RevisionNumber").longValue(),
                node.get("FeeReference").isNull() ? null : node.get("FeeReference").asText());
    }
}
