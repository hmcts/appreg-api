package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class ResolutionCodeDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private ResolutionCodeDiffService diffService;
    private ResolutionCodeDiffReportingService diffReportingService;
    private ResolutionCodeIngressDatabaseRowMapper rowMapper;
    private ResolutionCodeDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        properties.getProcessors().getResolutionCodes().setReportingDir(tempDir.toString());
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        rowMapper = new ResolutionCodeIngressDatabaseRowMapper();
        diffService = new ResolutionCodeDiffService(tableReadService, rowMapper);
        diffReportingService = new ResolutionCodeDiffReportingService(properties);
        processor =
                new ResolutionCodeDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        diffReportingService,
                        bulkUpsertService,
                        rowMapper);
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
    void given_countExceedsPageSize_when_retrieve_then_pagesThroughQueryEndpoint() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records");
        var secondPage = OBJECT_MAPPER.createObjectNode();
        secondPage.putArray("records");

        when(ingressClient.retrieveJson("/count/CSDS/ResolutionCode/GD")).thenReturn(countResponse);
        when(ingressClient.retrieveJson("/query/CSDS/ResolutionCode/GD?%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson("/query/CSDS/ResolutionCode/GD?%24limit=2&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_processorParametersConfigured_when_retrieve_then_appendsThemBeforePaging() {
        properties
                .getProcessors()
                .getResolutionCodes()
                .setParameters("?$f=PublishingStatus='Active'&$expr=Updator");

        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records");
        var secondPage = OBJECT_MAPPER.createObjectNode();
        secondPage.putArray("records");
        var parameterisedCountPath =
                "/count/CSDS/ResolutionCode/GD?$f=PublishingStatus='Active'&$expr=Updator";
        var parameterisedQueryPath =
                "/query/CSDS/ResolutionCode/GD?$f=PublishingStatus='Active'&$expr=Updator";

        when(ingressClient.retrieveJson(parameterisedCountPath)).thenReturn(countResponse);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=2&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_mockFileConfigured_when_retrieve_then_loadsMockResponseInsteadOfCallingClient()
            throws Exception {
        var mockFile = tempDir.resolve("resolution-codes.json");
        Files.writeString(
                mockFile,
                """
                {
                  "responseCode": 99,
                  "records": [
                    {
                      "ResolutionCodeID": 7,
                      "Code": "R7",
                      "ResultTitle": "Title 7",
                      "ResultWording": "Wording 7",
                      "Legislation": null,
                      "Recipient1Email": null,
                      "Recipient2Email": null,
                      "StartDate": "2020-01-01",
                      "EndDate": null,
                      "Notes": null,
                      "AuthoringStatus": "Published",
                      "PublishingStatus": "Active",
                      "CurrentRecordIndicator": true,
                      "DraftFinalExistsIndicator": false,
                      "RevisionNumber": 1,
                      "RevisionType": "Initial",
                      "RevisionDateFrom": null,
                      "RevisionDateTo": null,
                      "ClonedFrom": null,
                      "PSSRCID": null,
                      "PSSChangeSetHeaderID": null,
                      "PSSChangeSetItemID": null,
                      "FID_ApplicationRegisterHeader": 10193,
                      "FID_ReleasePackage": null,
                      "Updator": "migration"
                    }
                  ]
                }
                """);
        properties.getProcessors().getResolutionCodes().setMock(mockFile.toString());
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
                                        "Loaded CSDS mock response for resolution_codes from "
                                                + mockFile))
                .anyMatch(
                        log ->
                                log.contains(
                                        "Loaded mock CSDS payload for resolution_codes "
                                                + "with 1 records"));
    }

    @Test
    void given_missingMockFileConfigured_when_retrieve_then_logsAndFallsBackToEndpoint() {
        properties
                .getProcessors()
                .getResolutionCodes()
                .setMock("classpath:csds/does_not_exist.json");
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 1);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records");

        when(ingressClient.retrieveJson("/count/CSDS/ResolutionCode/GD")).thenReturn(countResponse);
        when(ingressClient.retrieveJson("/query/CSDS/ResolutionCode/GD?%24limit=2&%24offset=0"))
                .thenReturn(firstPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage);
        assertThat(logCaptor.getWarnLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Configured CSDS mock response for resolution_codes "
                                                + "was not found at "
                                                + "classpath:csds/does_not_exist.json. "
                                                + "Falling back to endpoint."));
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved 1 CSDS pages for resolution_codes"));
    }

    @Test
    void given_invalidMockFileConfigured_when_retrieve_then_logsErrorAndFallsBackToEndpoint()
            throws Exception {
        var invalidMockFile = tempDir.resolve("invalid-resolution-codes.json");
        Files.writeString(invalidMockFile, "{ not-json");
        properties.getProcessors().getResolutionCodes().setMock(invalidMockFile.toString());
        var logCaptor = LogCaptor.forClass(AbstractPagedCsdsIngressProcessor.class);
        logCaptor.clearLogs();
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 1);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records");

        when(ingressClient.retrieveJson("/count/CSDS/ResolutionCode/GD")).thenReturn(countResponse);
        when(ingressClient.retrieveJson("/query/CSDS/ResolutionCode/GD?%24limit=2&%24offset=0"))
                .thenReturn(firstPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage);
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to load CSDS mock response for resolution_codes from "
                                                + invalidMockFile
                                                + ". Falling back to endpoint."));
        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("Retrieved 1 CSDS pages for resolution_codes"));
    }

    @Test
    void
            given_processedData_withoutBulkRespondentAllowed_when_preProcess_then_addRcIdAndPreserveIncomingOrder() {
        var preProcessed =
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(3L, "RC3", "Title 3", "Wording 3", 1L),
                                        createSourceRecord(1L, "RC1", "Title 1", "Wording 1", 1L),
                                        createSourceRecord(
                                                2L, "RC2", "Title 2", "Wording 2", 1L))));

        assertThat(preProcessed).hasSize(1);
        assertThat(extractRecordsFromPage(preProcessed.getFirst()))
                .extracting(item -> item.get("RC_ID").longValue())
                .containsExactly(100003L, 100001L, 100002L);
        assertThat(extractRecordsFromPage(preProcessed.getFirst()))
                .extracting(item -> item.get("ResolutionCodeID").longValue())
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    void given_unmappedCsdsMetadataAbsent_when_preProcess_then_addRcId() {
        var sourceRecord = createSourceRecord(3L, "RC3", "Title 3", "Wording 3", 1L);
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

        assertThat(
                        extractRecordsFromPage(preProcessed.getFirst())
                                .getFirst()
                                .get("RC_ID")
                                .longValue())
                .isEqualTo(ResolutionCodeIngressRecord.calculateId(null, 3L));
    }

    @Test
    void
            given_processedData_when_apply_then_logsDiffAgainstExistingResolutionCodes_and_writesComparisonFiles()
                    throws Exception {
        var existingUpdated =
                new ResolutionCodeIngressRecord(
                        345L,
                        "R2",
                        "Title 2",
                        "Wording 2",
                        "Legislation 2",
                        "email1@example.com",
                        "email2@example.com",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        var existingUnmatched =
                new ResolutionCodeIngressRecord(
                        4L,
                        "R4",
                        "Title 4",
                        "Wording 4",
                        "Legislation 4",
                        null,
                        null,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper))
                .thenReturn(List.of(existingUpdated, existingUnmatched));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssrcid(
                                        345L, 2L, "R2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "R3", "Title 3", "Wording 3", 1L)));

        var logCaptor = LogCaptor.forClass(ResolutionCodeDiffReportingService.class);
        logCaptor.clearLogs();
        var processorLogCaptor = LogCaptor.forClass(ResolutionCodeDataIngressProcessor.class);
        processorLogCaptor.clearLogs();

        processor.apply(processor.preProcess(processedData));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=2, existing=2, inserts=1, updates=1"));
        assertThat(processorLogCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "CSDS ingress processor resolution_codes "
                                                + "produced inserts=1, updates=1"));

        try (var fileStream = Files.list(tempDir)) {
            var createdFiles = fileStream.toList();

            assertThat(createdFiles)
                    .extracting(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith("resolution_codes_incoming_"))
                    .anyMatch(name -> name.startsWith("resolution_codes_existing_"))
                    .anyMatch(name -> name.startsWith("resolution_codes_diff_"));

            var incomingCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                                    .toString()
                                                                    .startsWith(
                                                                            "resolution_codes_incoming_")
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
                                                                    "resolution_codes_existing_"))
                                    .findFirst()
                                    .orElseThrow());
            var diffCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .startsWith("resolution_codes_diff_"))
                                    .findFirst()
                                    .orElseThrow());

            assertThat(incomingCsv)
                    .contains(
                            "pssResolutionCodeId,resolutionCodeId,rcId,code,title,wording,legislation,"
                                    + "recipient1Email,recipient2Email,startDate,endDate,version")
                    .doesNotContain("bulkRespondentAllowed");
            assertThat(existingCsv)
                    .contains(
                            "pssResolutionCodeId,resolutionCodeId,rcId,code,title,wording,legislation,"
                                    + "recipient1Email,recipient2Email,startDate,endDate,version");
            assertThat(diffCsv)
                    .contains("pssResolutionCodeId,resolutionCodeId,rcId,changeType")
                    .contains("\"345\",\"2\",\"345\",\"update\"")
                    .contains(",\"3\",\"100003\",\"insert\"");
        }
    }

    @Test
    void given_processedData_when_ingest_then_returnsAppliedSummary() {
        var existingUpdated =
                new ResolutionCodeIngressRecord(
                        345L,
                        "R2",
                        "Title 2",
                        "Wording 2",
                        "Legislation 2",
                        "email1@example.com",
                        "email2@example.com",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        var existingUnmatched =
                new ResolutionCodeIngressRecord(
                        4L,
                        "R4",
                        "Title 4",
                        "Wording 4",
                        "Legislation 4",
                        null,
                        null,
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper))
                .thenReturn(List.of(existingUpdated, existingUnmatched));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssrcid(
                                        345L, 2L, "R2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "R3", "Title 3", "Wording 3", 1L)));

        var response = processor.ingest(processedData);

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isEqualTo(1);
    }

    @Test
    void given_processedData_when_diffCalculated_then_includeIntendedUpsertRecord() {
        var existingUpdated =
                new ResolutionCodeIngressRecord(
                        345L,
                        "R2",
                        "Title 2",
                        "Wording 2",
                        "Legislation 2",
                        "email1@example.com",
                        "email2@example.com",
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper))
                .thenReturn(List.of(existingUpdated));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssrcid(
                                        345L, 2L, "R2", "Updated Title", "Wording 2", 2L),
                                createSourceRecord(3L, "R3", "Title 3", "Wording 3", 1L)));

        var diffResult =
                diffService.diff(
                        new ResolutionCodeDiffRequest(
                                "resolution_codes_staging",
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
                            assertThat(diffRecord.intended().id())
                                    .isEqualTo(ResolutionCodeIngressRecord.calculateId(null, 3L));
                        });
    }

    @Test
    void given_pssrcidPresent_when_apply_then_useItAsTheResolvedKey() {
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper)).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(ResolutionCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecordWithoutResolutionCodeId(
                                                345L, "R3", "Title 3", "Wording 3", 1L)))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
    }

    @Test
    void given_pssrcidMissing_when_apply_then_useResolutionCodeIdOffsetKey() {
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper)).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(ResolutionCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(
                                                345L, "R3", "Title 3", "Wording 3", 1L)))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
    }

    @Test
    void given_revisionNumberPresent_when_apply_then_acceptLiveCsdsFieldName() {
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper)).thenReturn(List.of());
        var sourceRecord = createSourceRecord(345L, "R3", "Title 3", "Wording 3", 7L);

        var logCaptor = LogCaptor.forClass(ResolutionCodeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(processor.preProcess(List.of(createPageResponse(sourceRecord))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
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
                                createSourceRecord(345L, "R3", "Title 3", "Wording 3", 1L),
                                createSourceRecord(346L, "R4", "Title 4", "Wording 4", 1L)));
        var invalidRecord =
                ((ObjectNode) extractRecordsFromPage(processedData.getFirst()).getFirst());
        invalidRecord.remove("ResultTitle");

        assertThatThrownBy(() -> processor.preProcess(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("ResultTitle");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_countEndpoint_when_retrieve_then_callsItFirst() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 0);
        when(ingressClient.retrieveJson("/count/CSDS/ResolutionCode/GD")).thenReturn(countResponse);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).isEmpty();
        verify(ingressClient).retrieveJson("/count/CSDS/ResolutionCode/GD");
    }

    @Test
    void given_duplicateResolvedRcId_when_apply_then_throwException() {
        var logCaptor = LogCaptor.forClass(ResolutionCodeDiffService.class);
        logCaptor.clearLogs();

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssrcid(
                                        345L, 2L, "R2", "Title 2", "Wording 2", 2L),
                                createSourceRecordWithPssrcid(
                                        345L, 3L, "R3", "Title 3", "Wording 3", 1L)));
        var preProcessed = processor.preProcess(processedData);

        assertThatThrownBy(() -> processor.apply(preProcessed))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming RC_ID 345");
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                                "Duplicate incoming RC_ID 345 detected for resolution_codes_staging")
                                        && log.contains("duplicate record"));
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_reportingDirMissing_when_apply_then_skipDiffReportingWork() {
        properties.getProcessors().getResolutionCodes().setReportingDir(null);
        diffService = new ResolutionCodeDiffService(tableReadService, rowMapper);
        diffReportingService = new ResolutionCodeDiffReportingService(properties);
        processor =
                new ResolutionCodeDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        diffReportingService,
                        bulkUpsertService,
                        rowMapper);
        when(tableReadService.loadAll("resolution_codes_staging", rowMapper)).thenReturn(List.of());

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(
                                                1L, "R1", "Title 1", "Wording 1", 1L)))));

        verify(tableReadService).loadAll("resolution_codes_staging", rowMapper);
    }

    private ResolutionCodeIngressRecord toIngressRecord(JsonNode node) {
        return new ResolutionCodeIngressRecord(
                ResolutionCodeIngressRecord.resolveId(node),
                nullableText(node, "Code"),
                nullableText(node, "ResultTitle"),
                nullableText(node, "ResultWording"),
                nullableText(node, "Legislation"),
                nullableText(node, "Recipient1Email"),
                nullableText(node, "Recipient2Email"),
                nullableLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                nullableLong(node, "RevisionNumber"));
    }

    private ObjectNode createPageResponse(ObjectNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var recordsArray = page.putArray("records");
        for (var sourceRecord : records) {
            recordsArray.add(sourceRecord);
        }
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
                        .put("ResultTitle", title)
                        .put("ResultWording", wording)
                        .putNull("Legislation")
                        .putNull("Recipient1Email")
                        .putNull("Recipient2Email")
                        .put("StartDate", startDate)
                        .putNull("EndDate")
                        .putNull("Notes")
                        .put("AuthoringStatus", "Published")
                        .put("PublishingStatus", "Active")
                        .put("CurrentRecordIndicator", true)
                        .put("DraftFinalExistsIndicator", false)
                        .put("RevisionNumber", version)
                        .put("RevisionType", "Initial")
                        .putNull("RevisionDateFrom")
                        .putNull("RevisionDateTo")
                        .putNull("ClonedFrom")
                        .putNull("PSSRCID")
                        .putNull("PSSChangeSetHeaderID")
                        .putNull("PSSChangeSetItemID")
                        .put("FID_ApplicationRegisterHeader", 10193L)
                        .putNull("FID_ReleasePackage")
                        .put("Updator", "migration");
        if (id == null) {
            sourceRecord.putNull("ResolutionCodeID");
        } else {
            sourceRecord.put("ResolutionCodeID", id);
        }
        if (endDate != null) {
            sourceRecord.put("EndDate", endDate);
        }
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithoutResolutionCodeId(
            Long pssrcid, String code, String title, String wording, Long version) {
        var sourceRecord =
                createSourceRecord(null, code, title, wording, version, "2020-01-01", null);
        sourceRecord.put("PSSRCID", pssrcid);
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithPssrcid(
            Long pssrcid,
            Long resolutionCodeId,
            String code,
            String title,
            String wording,
            Long version) {
        var sourceRecord = createSourceRecord(resolutionCodeId, code, title, wording, version);
        sourceRecord.put("PSSRCID", pssrcid);
        return sourceRecord;
    }

    private List<JsonNode> extractRecordsFromPage(JsonNode page) {
        var records = page.get("records");
        var extracted = new ArrayList<JsonNode>();
        records.forEach(extracted::add);
        return List.copyOf(extracted);
    }

    private Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        if (field == null || !field.canConvertToLong()) {
            return null;
        }
        return field.longValue();
    }

    private String nullableText(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    private LocalDate nullableLocalDate(JsonNode node, String fieldName) {
        var value = nullableText(node, fieldName);
        return value == null ? null : LocalDate.parse(value);
    }
}
