package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.FailedUpsertRecord;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
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
    void given_pssId_when_preProcess_then_usesItAsNchId() {
        var processed =
                processor.preProcess(List.of(page(sourceRecord(3802L, 3106L, "Court", 1L))));

        assertThat(records(processed.getFirst()).getFirst().get("NCH_ID").longValue())
                .isEqualTo(3106L);
    }

    @Test
    void given_noPssId_when_preProcess_then_offsetsCourtId() {
        var processed = processor.preProcess(List.of(page(sourceRecord(3802L, null, "Court", 1L))));

        assertThat(records(processed.getFirst()).getFirst().get("NCH_ID").longValue())
                .isEqualTo(103802L);
    }

    @Test
    void given_existingAndNewCourts_when_ingest_then_upsertsAndReturnsSummary() throws Exception {
        var existing =
                new NationalCourtHouseIngressRecord(
                        3106L,
                        null,
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
                                        sourceRecord(3802L, 3106L, "Updated Court", 2L),
                                        sourceRecord(3803L, null, "New Court", 1L)
                                                .put("CourtLocationCode", "NEW"))));

        assertThat(response.getInserted()).isEqualTo(1);
        assertThat(response.getUpdated()).isEqualTo(1);
        verify(bulkUpsertService)
                .upsertBatch(
                        eq("national_court_houses_staging"),
                        eq("nch_id"),
                        argThat(
                                rows ->
                                        rows.size() == 2
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
            assertThat(Files.readString(incomingCsv)).contains("\"3106\"");
        }
    }

    @Test
    void given_duplicateRemoteId_when_ingest_then_rejectsBeforeDatabaseRead() {
        List<JsonNode> processedData =
                List.of(
                        page(
                                sourceRecord(3802L, 3106L, "First Court", 1L),
                                sourceRecord(3802L, 3106L, "Duplicate Court", 1L)
                                        .put("CourtLocationCode", "OTHER")));

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming CSDS_ID 3802");
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
                .hasMessageContaining("CourtID");
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
    void given_recordNodeIsNotObject_when_preProcess_then_preservesItForValidation() {
        var page = OBJECT_MAPPER.createObjectNode();
        page.putArray("records").add("invalid");
        List<JsonNode> processedData = List.of(page);

        assertThatThrownBy(() -> processor.ingest(processedData))
                .isInstanceOf(CsdsPayloadValidationException.class)
                .hasMessageContaining("missing expected fields");
    }

    @Test
    void given_emptyInput_when_preProcess_then_returnsEmptyInput() {
        assertThat(processor.preProcess(List.of())).isEmpty();
    }

    @Test
    void rejectsDuplicateCodesAcrossPagesIgnoringCase() {
        var first = sourceRecord(3802L, 3106L, "First", 1L);
        var second = sourceRecord(3803L, null, "Second", 1L).put("CourtLocationCode", "b01cf00");
        assertThatThrownBy(() -> processor.ingest(List.of(page(first), page(second))))
                .hasMessageContaining("Duplicate incoming CourtLocationCode");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void rejectsMissingBlankAndNonTextCodesBeforeWrites() throws Exception {
        for (var json : List.of("null", "\"\"", "\"   \"", "123", "true")) {
            var source = sourceRecord(3802L, null, "Court", 1L);
            source.set("CourtLocationCode", OBJECT_MAPPER.readTree(json));
            assertThatThrownBy(() -> processor.ingest(List.of(page(source))))
                    .hasMessageContaining("CourtLocationCode");
        }
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void rejectsInvalidIdentifiersEvenWithSpoofedLocalId() throws Exception {
        for (var field : List.of("CourtID", "PSSNationalCourthouseID")) {
            for (var json : List.of("0", "-1", "1.5", "\"3802\"", "true", "9223372036854775808")) {
                var source = sourceRecord(3802L, 3106L, "Court", 1L).put("NCH_ID", 1);
                source.set(field, OBJECT_MAPPER.readTree(json));
                assertThatThrownBy(() -> processor.ingest(List.of(page(source))))
                        .hasMessageContaining(field);
            }
        }
        var source = sourceRecord(2147483648L, 3106L, "Court", 1L);
        assertThatThrownBy(() -> processor.ingest(List.of(page(source))))
                .hasMessageContaining("CourtID");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void insertsSeparateFutureVersionAndReportsItsFinalId() throws Exception {
        var original =
                new NationalCourtHouseIngressRecord(
                        3106L,
                        3802L,
                        "Original",
                        1L,
                        LocalDate.of(2020, 1, 1),
                        null,
                        "B01CF00",
                        null);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(original));
        var source =
                sourceRecord(3803L, 3106L, "Future", 2L)
                        .put("StartDate", "2099-01-01")
                        .put("NCH_ID", 999);
        var pages = processor.preProcess(List.of(page(source)));
        var diff = processor.diff(pages);
        assertThat(diff.diffRecords())
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.intended().id()).isEqualTo(103803L);
                            assertThat(item.intended().csdsId()).isEqualTo(3803L);
                            assertThat(item.intended().startDate())
                                    .isEqualTo(LocalDate.of(2099, 1, 1));
                            assertThat(item.existing()).isNull();
                        });
        assertThat(processor.buildSuccessAudits(pages, diff).toString())
                .contains("103803", "3803", "Future");
        var failure =
                new CsdsBatchUpsertException(
                        "failed",
                        null,
                        List.of(
                                new FailedUpsertRecord<>(
                                        diff.diffRecords().getFirst().intended(), "failed row")));
        assertThat(processor.buildFailureAudits(pages, diff, failure).toString())
                .contains("103803", "3803", "Future", "failed row");
        processor.report(pages, diff);
        try (var files = Files.list(tempDir)) {
            var report =
                    files.filter(
                                    path ->
                                            path.getFileName()
                                                            .toString()
                                                            .startsWith(
                                                                    "national_court_houses_diff_")
                                                    && path.toString().endsWith(".csv"))
                            .findFirst()
                            .orElseThrow();
            assertThat(Files.readString(report)).contains("\"103803\"", "\"3803\"", "\"3106\"");
        }
    }

    @Test
    void enrichesUnlinkedOffsetIdWithoutPssIdAndSkipsReplay() {
        var original =
                new NationalCourtHouseIngressRecord(
                        103802L,
                        null,
                        "Original",
                        1L,
                        LocalDate.of(2020, 1, 1),
                        null,
                        "B01CF00",
                        null);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(original));
        var pages = processor.preProcess(List.of(page(sourceRecord(3802L, null, "Updated", 2L))));

        var diff = processor.diff(pages);

        assertThat(diff.diffRecords()).hasSize(1);
        var change = diff.diffRecords().getFirst();
        assertThat(change.operation()).isEqualTo(IngressOperation.UPDATE);
        assertThat(change.existing()).isEqualTo(original);
        assertThat(change.intended().id()).isEqualTo(103802L);
        assertThat(change.intended().csdsId()).isEqualTo(3802L);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(change.intended()));
        assertThat(processor.diff(pages).diffRecords()).isEmpty();
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(original));
        var mismatch = sourceRecord(3802L, null, "Other", 1L).put("CourtLocationCode", "OTHER");
        assertThatThrownBy(() -> processor.diff(processor.preProcess(List.of(page(mismatch)))))
                .hasMessageContaining("collides");
    }

    @Test
    void replayIsIgnoredButOffsetCollisionsAreRejected() {
        var original =
                new NationalCourtHouseIngressRecord(
                        103802L,
                        3802L,
                        "Original",
                        1L,
                        LocalDate.of(2020, 1, 1),
                        null,
                        "B01CF00",
                        null);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(original));
        var pages = processor.preProcess(List.of(page(sourceRecord(3802L, null, "Changed", 2L))));
        assertThat(processor.diff(pages).diffRecords()).isEmpty();
        var collision = sourceRecord(3802L, null, "Other", 1L).put("CourtLocationCode", "OTHER");
        assertThatThrownBy(() -> processor.ingest(List.of(page(collision))))
                .hasMessageContaining("Generated NCH_ID 103802 collides");
        verifyNoInteractions(bulkUpsertService);
    }

    @Test
    void mismatchedOrUncodedLegacyRowsAreNotOverwritten() {
        for (var oldCode : new String[] {"OTHER", null}) {
            var original =
                    new NationalCourtHouseIngressRecord(
                            3106L,
                            null,
                            "Original",
                            1L,
                            LocalDate.of(2020, 1, 1),
                            null,
                            oldCode,
                            null);
            when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                    .thenReturn(List.of(original));
            var pages = processor.preProcess(List.of(page(sourceRecord(3802L, 3106L, "New", 1L))));
            assertThat(processor.diff(pages).diffRecords())
                    .singleElement()
                    .satisfies(item -> assertThat(item.intended().id()).isEqualTo(103802L));
        }
    }

    private ObjectNode sourceRecord(Long courtId, Long pssId, String name, Long version) {
        var sourceRecord =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CourtName", name)
                        .putNull("CourtWelshName")
                        .put("CourtLocationCode", "B01CF00")
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
