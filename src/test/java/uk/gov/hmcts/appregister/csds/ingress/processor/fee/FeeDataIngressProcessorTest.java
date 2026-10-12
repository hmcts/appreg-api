package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsPayloadValidationException;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class FeeDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private FeeDiffService diffService;
    private FeeDiffReportingService diffReportingService;
    private FeeIngressDatabaseRowMapper rowMapper;
    private FeeDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        properties.getProcessors().getFee().setReportingDir(tempDir.toString());
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        rowMapper = new FeeIngressDatabaseRowMapper();
        diffService = new FeeDiffService(tableReadService, rowMapper);
        diffReportingService = new FeeDiffReportingService(properties);
        processor =
                new FeeDataIngressProcessor(
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

    private CsdsIngressTransactionRunner passthroughTransactionRunner() {
        return new CsdsIngressTransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> supplier) {
                return supplier.get();
            }
        };
    }

    @Test
    void given_processorParametersConfigured_when_retrieve_then_appendsThemBeforePaging() {
        properties
                .getProcessors()
                .getFee()
                .setParameters("?$f=AuthoringStatus='Published'&$expr=Updator");

        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage = OBJECT_MAPPER.createObjectNode();
        firstPage.putArray("records").addObject();
        firstPage.withArray("records").addObject();
        var secondPage = OBJECT_MAPPER.createObjectNode();
        secondPage.putArray("records").addObject();
        var parameterisedCountPath =
                "/count/APPREGISTER/CivilFee/GD?$f=AuthoringStatus='Published'&$expr=Updator";
        var parameterisedQueryPath =
                "/query/APPREGISTER/CivilFee/GD?$f=AuthoringStatus='Published'&$expr=Updator";

        when(ingressClient.retrieveJson(parameterisedCountPath)).thenReturn(countResponse);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(parameterisedQueryPath + "&%24limit=1&%24offset=2"))
                .thenReturn(secondPage);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_processedData_when_preProcess_then_preserveIncomingOrderWithoutResolvingIds() {
        var preProcessed =
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(3L, "CO3.1", "Fee 3", 1L),
                                        createSourceRecord(1L, "CO1.1", "Fee 1", 1L),
                                        createSourceRecord(2L, "CO2.1", "Fee 2", 1L))));

        assertThat(preProcessed).hasSize(1);
        assertThat(extractRecordsFromPage(preProcessed.getFirst()))
                .allSatisfy(item -> assertThat(item.has("FEE_ID")).isFalse());
        assertThat(extractRecordsFromPage(preProcessed.getFirst()))
                .extracting(item -> item.get("CivilFeeID").longValue())
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    void given_unmappedCsdsMetadataAbsent_when_preProcess_then_preserveSource() {
        var sourceRecord = createSourceRecord(3L, "CO3.1", "Fee 3", 1L);
        sourceRecord.remove(
                List.of(
                        "Notes",
                        "CurrentRecordIndicator",
                        "ClonedFrom",
                        "AuthoringStatus",
                        "PublishingStatus",
                        "DraftFinalExistsIndicator",
                        "RevisionDateFrom",
                        "RevisionDateTo",
                        "RevisionType",
                        "PSSChangeSetHeaderID",
                        "PSSChangeSetItemID",
                        "FID_FixedListHeader",
                        "FID_ReleasePackage",
                        "Updator"));

        var preProcessed = processor.preProcess(List.of(createPageResponse(sourceRecord)));

        assertThat(extractRecordsFromPage(preProcessed.getFirst()).getFirst())
                .isEqualTo(sourceRecord);
    }

    @ParameterizedTest
    @CsvSource({"FeeReference,StartDate", "IncomingReference,EffectiveFrom"})
    void given_processedData_when_apply_then_logsDiffAgainstExistingFees_and_writesComparisonFiles(
            String referenceField, String startDateField) throws Exception {
        properties
                .getProcessors()
                .getFee()
                .setBusinessKeys(List.of(referenceField, startDateField));
        when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.DEBUG);
        var existingUpdated =
                new FeeIngressRecord(
                        33L,
                        "CO10.1",
                        "Old Fee",
                        new BigDecimal("245.00"),
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        var existingUnmatched =
                new FeeIngressRecord(
                        99L,
                        "CO99.1",
                        "Fee 99",
                        new BigDecimal("300.00"),
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("fee_staging", rowMapper))
                .thenReturn(List.of(existingUpdated, existingUnmatched));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssFixedListId(
                                        33L, 696L, "CO10.1", "Updated Fee", 2L, "245.00"),
                                createSourceRecord(697L, "CO10.2", "New Fee", 1L)));

        var logCaptor = LogCaptor.forClass(FeeDiffReportingService.class);
        logCaptor.clearLogs();
        var processorLogCaptor = LogCaptor.forClass(FeeDataIngressProcessor.class);
        processorLogCaptor.clearLogs();

        processor.apply(processor.preProcess(processedData));

        verify(csdsAuditService)
                .persistSuccessAudits(
                        eq(CsdsAuditLevel.DEBUG),
                        argThat(
                                entries ->
                                        entries.size() == 2
                                                && entries.stream()
                                                        .allMatch(
                                                                entry ->
                                                                        entry.csdsJson()
                                                                                        .contains(
                                                                                                "\""
                                                                                                        + referenceField
                                                                                                        + "\":")
                                                                                && entry.csdsJson()
                                                                                        .contains(
                                                                                                "\""
                                                                                                        + startDateField
                                                                                                        + "\":"))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=2, existing=2, inserts=1, updates=1"));
        assertThat(processorLogCaptor.getInfoLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "CSDS ingress processor fee produced inserts=1, updates=1"));

        try (var fileStream = Files.list(tempDir)) {
            var createdFiles = fileStream.toList();

            assertThat(createdFiles)
                    .extracting(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith("fee_incoming_"))
                    .anyMatch(name -> name.startsWith("fee_existing_"))
                    .anyMatch(name -> name.startsWith("fee_diff_"));

            var incomingCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                                    .toString()
                                                                    .startsWith("fee_incoming_")
                                                            && path.getFileName()
                                                                    .toString()
                                                                    .endsWith(".csv"))
                                    .findFirst()
                                    .orElseThrow());
            var diffCsv =
                    Files.readString(
                            createdFiles.stream()
                                    .filter(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .startsWith("fee_diff_"))
                                    .findFirst()
                                    .orElseThrow());

            assertThat(incomingCsv)
                    .contains("pssFixedListId,civilFeeId,feeId,reference,description,amount")
                    .contains("\"33\",\"696\",\"33\",\"CO10.1\",\"Updated Fee\",\"245.00\"");
            assertThat(diffCsv)
                    .contains("pssFixedListId,civilFeeId,feeId,changeType")
                    .contains("\"33\",\"696\",\"33\",\"update\"")
                    .contains(
                            ",\"697\",\"%s\",\"insert\""
                                    .formatted(FeeIngressRecord.calculateId(null, 697L)));
        }
    }

    @Test
    void given_processedData_when_ingest_then_returnsAppliedSummary() {
        var existingUpdated =
                new FeeIngressRecord(
                        33L,
                        "CO10.1",
                        "Old Fee",
                        new BigDecimal("245.00"),
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("fee_staging", rowMapper))
                .thenReturn(List.of(existingUpdated));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssFixedListId(
                                        33L, 696L, "CO10.1", "Updated Fee", 2L, "245.00"),
                                createSourceRecord(697L, "CO10.2", "New Fee", 1L)));

        var response = processor.ingest(processedData);

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isEqualTo(1);
    }

    @Test
    void given_processedData_when_diffCalculated_then_includeIntendedUpsertRecord() {
        var existingUpdated =
                new FeeIngressRecord(
                        33L,
                        "CO10.1",
                        "Old Fee",
                        new BigDecimal("245.00"),
                        LocalDate.of(2020, Month.JANUARY, 1),
                        null,
                        1L);
        when(tableReadService.loadAll("fee_staging", rowMapper))
                .thenReturn(List.of(existingUpdated));

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithPssFixedListId(
                                        33L, 696L, "CO10.1", "Updated Fee", 2L, "245.00"),
                                createSourceRecord(697L, "CO10.2", "New Fee", 1L)));

        var diffResult =
                diffService.diff(
                        new FeeDiffRequest(
                                "fee_staging",
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
                            assertThat(diffRecord.intended().id()).isEqualTo(33L);
                        })
                .anySatisfy(
                        diffRecord -> {
                            assertThat(diffRecord.operation()).isEqualTo(IngressOperation.INSERT);
                            assertThat(diffRecord.existing()).isNull();
                            assertThat(diffRecord.intended()).isEqualTo(diffRecord.incoming());
                            assertThat(diffRecord.intended().id())
                                    .isEqualTo(FeeIngressRecord.calculateId(null, 697L));
                        });
    }

    @Test
    void given_pssFixedListIdPresent_when_apply_then_useItAsTheResolvedKey() {
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(FeeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecordWithoutCivilFeeId(
                                                33L, "CO10.1", "Fee 1", 1L, "245.00")))));

        assertThat(logCaptor.getInfoLogs())
                .anyMatch(log -> log.contains("incoming=1, existing=0, inserts=1, updates=0"));
    }

    @Test
    void given_pssFixedListIdMissing_when_apply_then_useCivilFeeIdAsTheResolvedKey() {
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of());

        var logCaptor = LogCaptor.forClass(FeeDiffReportingService.class);
        logCaptor.clearLogs();

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecord(696L, "CO10.1", "Fee 1", 1L)))));

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
                                createSourceRecord(696L, "CO10.1", "Fee 1", 1L),
                                createSourceRecord(697L, "CO10.2", "Fee 2", 1L)));
        var invalidRecord =
                ((ObjectNode) extractRecordsFromPage(processedData.getFirst()).getFirst());
        invalidRecord.remove("FeeReference");

        assertThatThrownBy(() -> processor.preProcess(processedData))
                .isInstanceOf(CsdsPayloadValidationException.class)
                .hasMessageContaining("FeeReference");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_sourceIdsMissingForExistingPair_when_ingest_then_preserveStoredId() {
        var sourceRecord = createSourceRecord(696L, "CO10.1", "Fee 1", 1L);
        sourceRecord.remove(List.of("PSSFixedListID", "CivilFeeID"));
        List<JsonNode> processedData = List.of(createPageResponse(sourceRecord));
        when(tableReadService.loadAll("fee_staging", rowMapper))
                .thenReturn(
                        List.of(
                                new FeeIngressRecord(
                                        33L,
                                        "CO10.1",
                                        "Old",
                                        BigDecimal.TEN,
                                        LocalDate.parse("2020-01-01"),
                                        null,
                                        1L)));
        assertThat(processor.ingest(processedData).getUpdated()).isEqualTo(1);
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("fee_staging"),
                        eq(List.of("fee_reference", "fee_start_date")),
                        argThat(rows -> rows.getFirst().id().equals(33L)),
                        same(rowMapper),
                        any());
    }

    @Test
    void given_invalidFeeValue_when_apply_then_throwException() {
        var sourceRecord = createSourceRecord(696L, "CO10.1", "Fee 1", 1L);
        sourceRecord.put("FeeValue", "not-a-number");
        var preProcessed = processor.preProcess(List.of(createPageResponse(sourceRecord)));

        assertThatThrownBy(() -> processor.apply(preProcessed))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("FeeValue");
    }

    @Test
    void given_versionNumberPresent_when_apply_then_acceptLegacyFeeCsdsFieldName() {
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of());

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecordWithVersionNumber(
                                                696L, "CO10.1", "Fee 1", 1L, "245.00")))));

        verify(tableReadService).loadAll("fee_staging", rowMapper);
    }

    @Test
    void given_revisionNumberNull_when_apply_then_defaultVersionToOne() {
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of());

        processor.apply(
                processor.preProcess(
                        List.of(
                                createPageResponse(
                                        createSourceRecordWithRevisionNumber(
                                                794L, "CO9.2", "Fee 1", null, "92.00")))));

        verify(tableReadService).loadAll("fee_staging", rowMapper);
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("fee_staging"),
                        eq(List.of("fee_reference", "fee_start_date")),
                        argThat(
                                rows ->
                                        rows.size() == 1
                                                && Long.valueOf(1L)
                                                        .equals(rows.getFirst().version())),
                        same(rowMapper),
                        any());
    }

    @Test
    void given_countEndpoint_when_retrieve_then_callsItFirst() {
        var countResponse = OBJECT_MAPPER.createObjectNode().put("count", 0);
        when(ingressClient.retrieveJson("/count/APPREGISTER/CivilFee/GD"))
                .thenReturn(countResponse);

        var retrieved = processor.retrieve(ingressClient);

        assertThat(retrieved).isEmpty();
        verify(ingressClient).retrieveJson("/count/APPREGISTER/CivilFee/GD");
    }

    @ParameterizedTest
    @CsvSource({"FeeReference,StartDate", "IncomingReference,EffectiveFrom"})
    void given_duplicateBusinessPair_when_apply_then_throwException(
            String referenceField, String startDateField) {
        properties
                .getProcessors()
                .getFee()
                .setBusinessKeys(List.of(referenceField, startDateField));
        for (var field : List.of(referenceField, startDateField)) {
            var missingField = createSourceRecord(101L, "FEE", "Description", 1L);
            missingField.remove(field);
            assertThatThrownBy(
                            () -> processor.preProcess(List.of(createPageResponse(missingField))))
                    .hasMessageContaining(field);
        }
        var logCaptor = LogCaptor.forClass(FeeDiffService.class);
        logCaptor.clearLogs();

        List<JsonNode> processedData =
                List.of(
                        createPageResponse(
                                createSourceRecordWithoutCivilFeeId(
                                        33L, "CO10.1", "Fee 1", 1L, "245.00"),
                                createSourceRecordWithoutCivilFeeId(
                                        34L, "CO10.1", "Fee 2", 2L, "250.00")));
        var preProcessed = processor.preProcess(processedData);

        assertThatThrownBy(() -> processor.apply(preProcessed))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming fee business key")
                .hasMessageContaining("Existing record [")
                .hasMessageContaining("duplicate record [");
        assertThat(logCaptor.getErrorLogs())
                .anyMatch(
                        log ->
                                log.contains("Duplicate incoming fee business key")
                                        && log.contains("duplicate record"));
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void changedAndOverflowingSourceIdsDoNotReplaceStoredIdentityOrAuditSource() {
        var existing =
                new FeeIngressRecord(
                        33L,
                        "CO10.1",
                        "Old",
                        BigDecimal.TEN,
                        LocalDate.parse("2020-01-01"),
                        null,
                        1L);
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of(existing));
        var source =
                createSourceRecord(Long.MAX_VALUE, "CO10.1", "Updated", 2L)
                        .put("PSSFixedListID", 9999L)
                        .put("FEE_ID", 8888L);
        when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.DEBUG);
        assertThat(processor.ingest(List.of(createPageResponse(source))).getUpdated()).isEqualTo(1);
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("fee_staging"),
                        eq(List.of("fee_reference", "fee_start_date")),
                        argThat(rows -> rows.getFirst().id().equals(33L)),
                        same(rowMapper),
                        any());
        assertThat(source.get("FEE_ID").longValue()).isEqualTo(8888L);
    }

    @ParameterizedTest
    @CsvSource({"FeeReference,StartDate", "IncomingReference,EffectiveFrom"})
    void sameReferenceWithDifferentStartsIsValidButRepeatedPairAcrossPagesIsNot(
            String referenceField, String startDateField) {
        properties
                .getProcessors()
                .getFee()
                .setBusinessKeys(List.of(referenceField, startDateField));
        var first = createSourceRecord(1L, "PAIR", "First", 1L);
        var second = createSourceRecord(2L, "PAIR", "Second", 1L).put(startDateField, "2021-01-01");
        assertThat(processor.ingest(List.of(createPageResponse(first, second))).getInserted())
                .isEqualTo(2);
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(
                                                createPageResponse(first),
                                                createPageResponse(
                                                        first.deepCopy().put("CivilFeeID", 3L)))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming fee business key");
    }

    @Test
    void invalidNewIdsRejectButProvidedLegacyIdsSkipGeneratedCollisionChecks() {
        var owner =
                new FeeIngressRecord(
                        100033L,
                        "OWNER",
                        "Old",
                        BigDecimal.TEN,
                        LocalDate.parse("2020-01-01"),
                        null,
                        1L);
        when(tableReadService.loadAll("fee_staging", rowMapper)).thenReturn(List.of(owner));
        var valid = createSourceRecord(1L, "VALID", "Valid", 1L);
        var missing = createSourceRecord(null, "MISSING", "Missing", 1L);
        var overflow = createSourceRecord(Long.MAX_VALUE, "OVERFLOW", "Overflow", 1L);
        var generatedCollision = createSourceRecord(33L, "OTHER", "Other", 1L);
        final var incomingCollision =
                createSourceRecordWithoutCivilFeeId(100001L, "OTHER", "Other", 1L, "1");
        for (var invalid :
                List.of(missing, missing.deepCopy().put("CivilFeeID", "invalid"), overflow)) {
            assertThatThrownBy(
                            () ->
                                    processor.ingest(
                                            List.of(
                                                    createPageResponse(valid),
                                                    createPageResponse(invalid))))
                    .isInstanceOf(
                            invalid == overflow
                                    ? ArithmeticException.class
                                    : AppRegistryException.class);
        }
        verifyNoInteractions(bulkUpsertService);

        assertThatThrownBy(
                        () ->
                                processor.diff(
                                        processor.preProcess(
                                                List.of(
                                                        createPageResponse(valid),
                                                        createPageResponse(generatedCollision)))))
                .hasMessageContaining("Generated legacy FEE_ID 100033");

        var incomingCollisionDiff =
                processor.diff(
                        processor.preProcess(
                                List.of(
                                        createPageResponse(valid),
                                        createPageResponse(incomingCollision))));
        assertThat(incomingCollisionDiff.diffRecords()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"StartDate", "EffectiveFrom"})
    void ukTodayAndExpiredAreRetainedTomorrowIsWarnedAndExcludedBeforeIdentityChecks(
            String startDateField) {
        properties
                .getProcessors()
                .getFee()
                .setBusinessKeys(List.of("FeeReference", startDateField));
        var today = createSourceRecord(1L, "DATE", "Today", 1L).put(startDateField, "2026-07-02");
        var future =
                today.deepCopy()
                        .put(startDateField, "2026-07-03")
                        .put("CivilFeeID", Long.MAX_VALUE);
        var expired = createSourceRecord(2L, "OLD", "Expired", 1L).put("EndDate", "2021-01-01");
        try (var logs = LogCaptor.forClass(FeeDataIngressProcessor.class)) {
            var retained =
                    processor.preProcess(
                            List.of(
                                    createPageResponse(today),
                                    createPageResponse(future, expired)));
            assertThat(extractRecordsFromPage(retained.getFirst())).containsExactly(today, expired);
            assertThat(logs.getWarnLogs())
                    .singleElement()
                    .asString()
                    .contains("DATE", "2026-07-03", "2026-07-02");
        }
        assertThat(
                        processor
                                .ingest(
                                        List.of(
                                                createPageResponse(
                                                        today, future, future.deepCopy())))
                                .getInserted())
                .isEqualTo(1);
        assertThat(
                        processor
                                .ingest(List.of(createPageResponse(future, future.deepCopy())))
                                .getInserted())
                .isZero();
        assertThat(processor.preProcess(List.of())).isEmpty();
    }

    @Test
    void everyDateIsValidatedIncludingFutureRecordsAndLaterPagesBeforeAnyDatabaseAccess() {
        var valid = createSourceRecord(1L, "VALID", "Valid", 1L);
        var missing = valid.deepCopy();
        missing.remove("StartDate");
        for (var invalid :
                List.of(
                        missing,
                        valid.deepCopy().putNull("StartDate"),
                        valid.deepCopy().put("StartDate", ""),
                        valid.deepCopy().put("StartDate", "2026-02-30"),
                        valid.deepCopy().put("StartDate", "2026-07-03").put("EndDate", "invalid"),
                        valid.deepCopy().put("EndDate", ""))) {
            assertThatThrownBy(
                            () ->
                                    processor.ingest(
                                            List.of(
                                                    createPageResponse(valid),
                                                    createPageResponse(invalid))))
                    .isInstanceOf(AppRegistryException.class);
        }
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    private FeeIngressRecord toIngressRecord(JsonNode node) {
        return new FeeIngressRecord(
                nullableLong(node, "CivilFeeID"),
                nullableText(node, "FeeReference"),
                nullableText(node, "Description"),
                nullableBigDecimal(node, "FeeValue"),
                nullableLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                resolvedVersion(node),
                nullableLong(node, "PSSFixedListID"));
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
            Long civilFeeId, String feeReference, String description, Long version) {
        return createSourceRecord(civilFeeId, feeReference, description, version, "245.00");
    }

    private ObjectNode createSourceRecord(
            Long civilFeeId,
            String feeReference,
            String description,
            Long version,
            String feeValue) {
        return createSourceRecordWithVersionField(
                civilFeeId, feeReference, description, version, feeValue, "RevisionNumber");
    }

    private ObjectNode createSourceRecordWithRevisionNumber(
            Long civilFeeId,
            String feeReference,
            String description,
            Long version,
            String feeValue) {
        return createSourceRecordWithVersionField(
                civilFeeId, feeReference, description, version, feeValue, "RevisionNumber");
    }

    private ObjectNode createSourceRecordWithVersionNumber(
            Long civilFeeId,
            String feeReference,
            String description,
            Long version,
            String feeValue) {
        return createSourceRecordWithVersionField(
                civilFeeId, feeReference, description, version, feeValue, "VersionNumber");
    }

    private ObjectNode createSourceRecordWithVersionField(
            Long civilFeeId,
            String feeReference,
            String description,
            Long version,
            String feeValue,
            String versionFieldName) {
        var sourceRecord =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put(
                                properties.getProcessors().getFee().getBusinessKeys().getFirst(),
                                feeReference)
                        .put("Description", description)
                        .put("FeeValue", feeValue)
                        .put(
                                properties.getProcessors().getFee().getBusinessKeys().get(1),
                                "2020-01-01")
                        .putNull("EndDate")
                        .putNull("Notes")
                        .put("CurrentRecordIndicator", false)
                        .putNull("ClonedFrom")
                        .put("AuthoringStatus", "Published")
                        .put("PublishingStatus", "Inactive")
                        .put("DraftFinalExistsIndicator", false)
                        .putNull("RevisionDateFrom")
                        .putNull("RevisionDateTo")
                        .putNull("RevisionNumber")
                        .putNull("RevisionType")
                        .putNull("VersionNumber")
                        .putNull("VersionType")
                        .putNull("PSSFixedListID")
                        .putNull("PSSChangeSetHeaderID")
                        .putNull("PSSChangeSetItemID")
                        .put("FID_FixedListHeader", 171576L)
                        .putNull("FID_ReleasePackage")
                        .put("Updator", "migration");
        putNullableLong(sourceRecord, versionFieldName, version);
        if ("RevisionNumber".equals(versionFieldName)) {
            sourceRecord.put("RevisionType", "New");
        } else {
            sourceRecord.put("VersionType", "New");
        }
        if (civilFeeId == null) {
            sourceRecord.putNull("CivilFeeID");
        } else {
            sourceRecord.put("CivilFeeID", civilFeeId);
        }
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithPssFixedListId(
            Long pssFixedListId,
            Long civilFeeId,
            String feeReference,
            String description,
            Long version,
            String feeValue) {
        var sourceRecord =
                createSourceRecord(civilFeeId, feeReference, description, version, feeValue);
        sourceRecord.put("PSSFixedListID", pssFixedListId);
        return sourceRecord;
    }

    private ObjectNode createSourceRecordWithoutCivilFeeId(
            Long pssFixedListId,
            String feeReference,
            String description,
            Long version,
            String feeValue) {
        return createSourceRecordWithPssFixedListId(
                pssFixedListId, null, feeReference, description, version, feeValue);
    }

    private Long resolvedVersion(JsonNode node) {
        var revisionNumber = nullableLong(node, "RevisionNumber");
        if (revisionNumber != null) {
            return revisionNumber;
        }

        var versionNumber = nullableLong(node, "VersionNumber");
        if (versionNumber != null) {
            return versionNumber;
        }

        return 1L;
    }

    private void putNullableLong(ObjectNode sourceRecord, String fieldName, Long value) {
        if (value == null) {
            sourceRecord.putNull(fieldName);
            return;
        }

        sourceRecord.put(fieldName, value);
    }

    private List<JsonNode> extractRecordsFromPage(JsonNode page) {
        var records = new ArrayList<JsonNode>();
        page.get("records").forEach(records::add);
        return List.copyOf(records);
    }

    private String nullableText(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    private Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return field == null || !field.canConvertToLong() ? null : field.longValue();
    }

    private BigDecimal nullableBigDecimal(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return new BigDecimal(field.asText());
    }

    private LocalDate nullableLocalDate(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return LocalDate.parse(field.asText());
    }
}
