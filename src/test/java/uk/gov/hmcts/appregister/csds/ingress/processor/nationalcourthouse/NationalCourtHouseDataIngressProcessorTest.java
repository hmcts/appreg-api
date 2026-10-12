package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.FailedUpsertRecord;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.exception.CsdsPayloadValidationException;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    @TempDir Path tempDir;

    private CsdsIngressProperties properties;
    private NationalCourtHouseIngressDatabaseRowMapper rowMapper;
    private NationalCourtHouseDataIngressProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        properties.getProcessors().getNationalCourtHouses().setReportingDir(tempDir.toString());
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        rowMapper = new NationalCourtHouseIngressDatabaseRowMapper();
        var diffService = new NationalCourtHouseDiffService(tableReadService, rowMapper);
        processor =
                new NationalCourtHouseDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        diffService,
                        new NationalCourtHouseDiffReportingService(properties),
                        bulkUpsertService,
                        rowMapper,
                        Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneOffset.UTC));
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
    void given_configuredProcessor_when_retrieve_then_countsAndFetchesPages() {
        properties
                .getProcessors()
                .getNationalCourtHouses()
                .setParameters(
                        "?$f=PublishingStatus='Active'&$f=CurrentRecordIndicator='true'"
                                + "&$f=CourtHearingOperationAreaIndicator='true'&$orderBy=CourtID");
        var count = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var firstPage =
                page(
                        sourceRecord(3802L, 3106L, "First Court", 1L),
                        sourceRecord(3804L, null, "Third Court", 1L));
        var secondPage = page(sourceRecord(3803L, null, "Second Court", 1L));
        var parameters =
                "?$f=PublishingStatus='Active'&$f=CurrentRecordIndicator='true'"
                        + "&$f=CourtHearingOperationAreaIndicator='true'&$orderBy=CourtID";
        var countPath = "/count/COURT/Court/GD" + parameters;
        var queryPath = "/query/COURT/Court/GD" + parameters;
        when(ingressClient.retrieveJson(countPath)).thenReturn(count);
        when(ingressClient.retrieveJson(queryPath + "&%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(queryPath + "&%24limit=1&%24offset=2"))
                .thenReturn(secondPage);

        assertThat(processor.retrieve(ingressClient)).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_pssId_when_diff_then_usesItForNewLocationCode() {
        var processed =
                processor.preProcess(List.of(page(sourceRecord(3802L, 3106L, "Court", 1L))));

        assertThat(processor.diff(processed).incomingById()).containsOnlyKeys(3106L);
        assertThat(records(processed.getFirst()).getFirst().has("NCH_ID")).isFalse();
    }

    @Test
    void given_noPssId_when_diff_then_offsetsCourtIdForNewLocationCode() {
        var processed = processor.preProcess(List.of(page(sourceRecord(3802L, null, "Court", 1L))));

        assertThat(processor.diff(processed).incomingById()).containsOnlyKeys(103802L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CourtLocationCode", "IncomingLocationCode"})
    void given_existingAndNewCourts_when_ingest_then_upsertsAndReturnsSummary(String codeField)
            throws Exception {
        properties.getProcessors().getNationalCourtHouses().setBusinessKeys(List.of(codeField));
        when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.DEBUG);
        var existing =
                new NationalCourtHouseIngressRecord(
                        3106L,
                        "Old Court",
                        1L,
                        LocalDate.of(1900, Month.JANUARY, 1),
                        null,
                        "B01CF00",
                        null);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(existing));

        var response =
                processor.ingest(
                        List.of(
                                page(
                                        sourceRecord(3802L, 9999L, "Shared Court", 2L),
                                        sourceRecord(3803L, null, "Shared Court", 1L)
                                                .put(codeField, "B02CF00"))));

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isEqualTo(1);
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
                                                                                                + codeField
                                                                                                + "\":"))));
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("national_court_houses_staging"),
                        eq(List.of("court_location_code")),
                        argThat(
                                rows ->
                                        rows.size() == 2
                                                && rows.stream()
                                                        .allMatch(
                                                                row ->
                                                                        row.name()
                                                                                .equals(
                                                                                        "Shared Court"))
                                                && rows.stream()
                                                        .map(NationalCourtHouseIngressRecord::id)
                                                        .toList()
                                                        .equals(List.of(3106L, 103803L))),
                        same(rowMapper),
                        org.mockito.ArgumentMatchers.any());
        try (var files = Files.list(tempDir)) {
            var generatedFiles = files.toList();
            assertThat(generatedFiles.stream().map(path -> path.getFileName().toString()).toList())
                    .anyMatch(name -> name.startsWith("national_court_houses_incoming_"))
                    .anyMatch(name -> name.startsWith("national_court_houses_existing_"))
                    .anyMatch(name -> name.startsWith("national_court_houses_diff_"));
            var incomingCsv =
                    generatedFiles.stream()
                            .filter(
                                    path ->
                                            path.getFileName()
                                                            .toString()
                                                            .startsWith(
                                                                    "national_court_houses_incoming_")
                                                    && path.getFileName()
                                                            .toString()
                                                            .endsWith(".csv"))
                            .findFirst()
                            .orElseThrow();
            assertThat(Files.readString(incomingCsv))
                    .contains(
                            "\"3106\"",
                            "\"103803\"",
                            "\"Shared Court\"",
                            "\"B01CF00\"",
                            "\"B02CF00\"");
            var diffCsv =
                    generatedFiles.stream()
                            .filter(
                                    path ->
                                            path.getFileName()
                                                            .toString()
                                                            .startsWith(
                                                                    "national_court_houses_diff_")
                                                    && path.toString().endsWith(".csv"))
                            .findFirst()
                            .orElseThrow();
            assertThat(Files.readString(diffCsv))
                    .contains("\"9999\",\"3802\",\"3106\",\"update\"")
                    .contains("\"3803\",\"103803\",\"insert\"");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CourtLocationCode", "IncomingLocationCode"})
    void given_duplicateCodesAcrossPages_when_ingest_then_rejectsBeforeDatabaseRead(
            String codeField) {
        properties.getProcessors().getNationalCourtHouses().setBusinessKeys(List.of(codeField));
        var missingField = sourceRecord(3802L, 3106L, "Court", 1L);
        missingField.remove(codeField);
        assertThatThrownBy(() -> processor.preProcess(List.of(page(missingField))))
                .hasMessageContaining(codeField);
        List<JsonNode> processedData =
                List.of(
                        page(sourceRecord(3802L, 3106L, "Court", 1L)),
                        page(sourceRecord(3803L, 3107L, "Different Court", 1L)));

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming court_location_code B01CF00")
                .hasMessageContaining("Existing record [")
                .hasMessageContaining("duplicate record [")
                .hasMessageContaining("name=Court")
                .hasMessageContaining("name=Different Court");
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(
                                                page(
                                                        sourceRecord(3802L, 3106L, "Court", 1L),
                                                        sourceRecord(3803L, 3107L, "Court", 1L)))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming court_location_code B01CF00");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_missingRequiredField_when_preProcess_then_rejectsRecord() {
        var sourceRecord = sourceRecord(3802L, 3106L, "Court", 1L);
        sourceRecord.remove("CourtName");
        List<JsonNode> processedData = List.of(page(sourceRecord));

        assertThatThrownBy(() -> processor.preProcess(processedData))
                .isInstanceOf(CsdsPayloadValidationException.class)
                .hasMessageContaining("CourtName");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_noResolvableId_when_ingest_then_rejectsRecord() {
        var sourceRecord = sourceRecord(null, null, "Court", 1L);
        List<JsonNode> processedData = List.of(page(sourceRecord));

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Missing source ID for new national courthouse");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void failedUpsertAuditsSameNameCourtsByTheirLocationCodes() {
        var first =
                sourceRecord(3802L, null, "Shrewsbury Crown Court", 1L)
                        .put("CourtLocationCode", "C22SR00");
        var second =
                sourceRecord(3803L, null, "Shrewsbury Crown Court", 1L)
                        .put("CourtLocationCode", "C22SW00");
        doAnswer(
                        invocation -> {
                            List<NationalCourtHouseIngressRecord> rows = invocation.getArgument(2);
                            throw new CsdsBatchUpsertException(
                                    "write failed",
                                    new IllegalStateException("write failed"),
                                    List.of(
                                            new FailedUpsertRecord<>(
                                                    rows.getFirst(), "write failed"),
                                            new FailedUpsertRecord<>(rows.get(1), "write failed")));
                        })
                .when(bulkUpsertService)
                .upsertBatch(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> processor.ingest(List.of(page(first, second))))
                .isInstanceOf(CsdsBatchUpsertException.class)
                .hasMessage("write failed");
        verify(csdsAuditService)
                .persistFailureAudits(
                        any(),
                        argThat(
                                entries ->
                                        entries.size() == 2
                                                && entries.getFirst().appregKey().equals(103802L)
                                                && entries.getFirst()
                                                        .csdsJson()
                                                        .equals(first.toString())
                                                && entries.get(1).appregKey().equals(103803L)
                                                && entries.get(1)
                                                        .csdsJson()
                                                        .equals(second.toString())));
    }

    @Test
    void malformedIdForNewLocationCodeIsRejectedBeforeWrites() {
        var source = sourceRecord(null, null, "Malformed", 1L).put("CourtID", "not-a-number");
        assertThatThrownBy(() -> processor.ingest(List.of(page(source))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Missing source ID for new national courthouse B01CF00");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void given_invalidDate_when_ingest_then_rejectsRecord() {
        var sourceRecord = sourceRecord(3802L, 3106L, "Court", 1L).put("StartDate", "invalid");
        List<JsonNode> processedData = List.of(page(sourceRecord));

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("StartDate");
    }

    @Test
    void given_recordNodeIsNotObject_when_preProcess_then_rejectsIt() {
        var page = OBJECT_MAPPER.createObjectNode();
        page.putArray("records").add("invalid");
        List<JsonNode> processedData = List.of(page);

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("StartDate");
    }

    @Test
    void given_emptyInput_when_preProcess_then_returnsEmptyInput() {
        assertThat(processor.preProcess(List.of())).isEmpty();
    }

    @Test
    void existingCodeRetainsStoredIdWithRenamedCourtAndChangedMissingAndOverflowingSourceIds() {
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(
                        List.of(
                                new NationalCourtHouseIngressRecord(
                                        42L,
                                        "Court",
                                        1L,
                                        LocalDate.parse("1900-01-01"),
                                        null,
                                        "B01CF00",
                                        null)));
        for (var source :
                List.of(
                        sourceRecord(1L, 999L, "Renamed Court", 2L),
                        sourceRecord(Long.MAX_VALUE, null, "Renamed Court", 2L),
                        sourceRecord(null, null, "Renamed Court", 2L))) {
            if (source.path("CourtID").isNull()) {
                source.remove(List.of("CourtID", "PSSNationalCourthouseID"));
            }
            var diff = processor.diff(processor.preProcess(List.of(page(source))));
            assertThat(diff.incomingById()).containsOnlyKeys(42L);
            assertThat(diff.incomingById().get(42L).version()).isEqualTo(2L);
            assertThat(diff.incomingById().get(42L).name()).isEqualTo("Renamed Court");
            assertThat(diff.diffRecords().getFirst().existing().id()).isEqualTo(42L);
        }
    }

    @Test
    void sameNameWithDifferentLocationCodesMatchesDistinctExistingCourts() {
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(
                        List.of(
                                new NationalCourtHouseIngressRecord(
                                        42L,
                                        "Shrewsbury Crown Court",
                                        1L,
                                        LocalDate.parse("1900-01-01"),
                                        null,
                                        "C22SR00",
                                        null),
                                new NationalCourtHouseIngressRecord(
                                        43L,
                                        "Shrewsbury Crown Court",
                                        1L,
                                        LocalDate.parse("1900-01-01"),
                                        null,
                                        "C22SW00",
                                        null)));
        var diff =
                processor.diff(
                        processor.preProcess(
                                List.of(
                                        page(
                                                sourceRecord(1L, null, "Shrewsbury Crown Court", 2L)
                                                        .put("CourtLocationCode", "C22SR00"),
                                                sourceRecord(2L, null, "Shrewsbury Crown Court", 2L)
                                                        .put("CourtLocationCode", "C22SW00")))));
        assertThat(diff.incomingById()).containsOnlyKeys(42L, 43L);
        assertThat(diff.incomingById().values())
                .extracting(NationalCourtHouseIngressRecord::courtLocationCode)
                .containsExactly("C22SR00", "C22SW00");
    }

    @Test
    void locationCodeMatchingRemainsCaseSensitive() {
        var diff =
                processor.diff(
                        processor.preProcess(
                                List.of(
                                        page(
                                                sourceRecord(1L, null, "Court", 1L)
                                                        .put("CourtLocationCode", "C22SR00"),
                                                sourceRecord(2L, null, "Court", 1L)
                                                        .put("CourtLocationCode", "c22sr00")))));
        assertThat(diff.incomingById()).containsOnlyKeys(100001L, 100002L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void missingOrBlankLocationCodeIsRejectedBeforeWrites(String code) {
        var source = sourceRecord(1L, null, "Court", 1L).put("CourtLocationCode", code);
        assertThatThrownBy(() -> processor.ingest(List.of(page(source))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("CourtLocationCode");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void newIdOverflowFailsButProvidedLegacyIdsSkipGeneratedCollisionChecks() {
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(
                                                page(
                                                        sourceRecord(
                                                                Long.MAX_VALUE,
                                                                null,
                                                                "Overflow",
                                                                1L)))))
                .isInstanceOf(ArithmeticException.class);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(
                        List.of(
                                new NationalCourtHouseIngressRecord(
                                        100033L,
                                        "Stored",
                                        1L,
                                        LocalDate.parse("1900-01-01"),
                                        null,
                                        "STORED",
                                        null)));
        assertThatThrownBy(
                        () ->
                                processor.diff(
                                        processor.preProcess(
                                                List.of(
                                                        page(
                                                                sourceRecord(
                                                                        33L, null, "Stored",
                                                                        1L))))))
                .hasMessageContaining("Generated legacy NCH_ID 100033");

        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of());
        var incomingCollision =
                processor.diff(
                        processor.preProcess(
                                List.of(
                                        page(
                                                sourceRecord(1L, null, "Fallback", 1L),
                                                sourceRecord(2L, 100001L, "PSS collision", 1L)
                                                        .put("CourtLocationCode", "B02CF00")))));
        assertThat(incomingCollision.diffRecords()).hasSize(2);
    }

    @Test
    void futureRecordsAreWarnedAndDroppedBeforeIdentityAndDuplicatesAtUkMidnight() {
        var today = sourceRecord(1L, null, "Court", 1L).put("StartDate", "2026-07-02");
        var future = sourceRecord(Long.MAX_VALUE, null, "Court", 1L).put("StartDate", "2026-07-03");
        try (var logs = LogCaptor.forClass(NationalCourtHouseDataIngressProcessor.class)) {
            var processed = processor.preProcess(List.of(page(today), page(future)));
            assertThat(records(processed.getFirst())).hasSize(1);
            assertThat(processor.diff(processed).incomingById()).containsOnlyKeys(100001L);
            assertThat(logs.getWarnLogs())
                    .anyMatch(
                            log ->
                                    log.contains("B01CF00")
                                            && log.contains("2026-07-03")
                                            && log.contains("today 2026-07-02"));
            var allFuture = processor.diff(processor.preProcess(List.of(page(future))));
            assertThat(allFuture.incomingById()).isEmpty();
            assertThat(allFuture.diffRecords()).isEmpty();
            assertThat(logs.getWarnLogs()).hasSize(2);
        }
        assertThat(
                        records(
                                processor
                                        .preProcess(
                                                List.of(
                                                        page(
                                                                today.deepCopy()
                                                                        .put(
                                                                                "StartDate",
                                                                                "2020-01-01")
                                                                        .put(
                                                                                "EndDate",
                                                                                "2020-12-31"))))
                                        .getFirst()))
                .hasSize(1);
        assertThat(today.has("NCH_ID")).isFalse();
    }

    @Test
    void invalidDatesOnLaterPagesFailIncludingFutureRecords() {
        var valid = sourceRecord(1L, null, "Valid", 1L);
        for (var start : List.of("", "not-a-date", "2026-02-30")) {
            var invalid = valid.deepCopy().put("StartDate", start);
            assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(invalid))))
                    .isInstanceOf(AppRegistryException.class)
                    .hasMessageContaining("StartDate");
        }
        for (var invalid : List.of(valid.deepCopy().putNull("StartDate"), valid.deepCopy())) {
            if (!invalid.path("StartDate").isNull()) {
                invalid.remove("StartDate");
            }
            assertThatThrownBy(() -> processor.ingest(List.of(page(invalid))))
                    .isInstanceOf(AppRegistryException.class)
                    .hasMessageContaining("StartDate");
        }
        for (var end : List.of("", "not-a-date", "2026-02-30")) {
            var invalid = valid.deepCopy().put("StartDate", "2026-07-03").put("EndDate", end);
            assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(invalid))))
                    .isInstanceOf(AppRegistryException.class)
                    .hasMessageContaining("EndDate");
        }
        verifyNoInteractions(bulkUpsertService);
    }

    private ObjectNode sourceRecord(Long courtId, Long pssId, String name, Long version) {
        var sourceRecord =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CourtName", name)
                        .put(
                                properties
                                        .getProcessors()
                                        .getNationalCourtHouses()
                                        .getBusinessKeys()
                                        .getFirst(),
                                "B01CF00")
                        .putNull("CourtWelshName")
                        .put("StartDate", "1900-01-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", version);
        sourceRecord.put("CourtID", courtId);
        sourceRecord.put("PSSNationalCourthouseID", pssId);
        return sourceRecord;
    }

    private ObjectNode page(ObjectNode... sourceRecords) {
        var page = OBJECT_MAPPER.createObjectNode();
        var records = page.putArray("records");
        for (var sourceRecord : sourceRecords) {
            records.add(sourceRecord);
        }
        return page;
    }

    private List<JsonNode> records(JsonNode page) {
        var records = new ArrayList<JsonNode>();
        page.get("records").forEach(records::add);
        return List.copyOf(records);
    }
}
