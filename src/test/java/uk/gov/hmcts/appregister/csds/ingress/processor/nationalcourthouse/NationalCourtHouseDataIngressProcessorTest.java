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
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class NationalCourtHouseDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private JdbcBulkUpsertService bulkUpsertService;
    @Mock private CsdsAuditService csdsAuditService;

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
        var firstPage = page(sourceRecord(3802L, 3106L, "First Court", 1L));
        var secondPage = page(sourceRecord(3803L, null, "Second Court", 1L));
        var parameters =
                "?$f=PublishingStatus='Active'&$f=CurrentRecordIndicator='true'"
                        + "&$f=CourtHearingOperationAreaIndicator='true'&$orderBy=CourtID";
        var countPath = "/count/CSDS/Court/GD" + parameters;
        var queryPath = "/query/CSDS/Court/GD" + parameters;
        when(ingressClient.retrieveJson(countPath)).thenReturn(count);
        when(ingressClient.retrieveJson(queryPath + "&%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(queryPath + "&%24limit=2&%24offset=2"))
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
                        3106L, "Old Court", 1L, LocalDate.of(1900, 1, 1), null, "OLD", null);
        when(tableReadService.loadAll("national_court_houses_staging", rowMapper))
                .thenReturn(List.of(existing));

        var response =
                processor.ingest(
                        List.of(
                                page(
                                        sourceRecord(3802L, 3106L, "Updated Court", 2L),
                                        sourceRecord(3803L, null, "New Court", 1L))));

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
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .anyMatch(name -> name.startsWith("national_court_houses_incoming_"))
                    .anyMatch(name -> name.startsWith("national_court_houses_existing_"))
                    .anyMatch(name -> name.startsWith("national_court_houses_diff_"));
        }
    }

    @Test
    void given_duplicateResolvedId_when_ingest_then_rejectsBeforeDatabaseRead() {
        List<JsonNode> records =
                List.of(
                        page(
                                sourceRecord(3802L, 3106L, "First Court", 1L),
                                sourceRecord(3803L, 3106L, "Duplicate Court", 1L)));

        assertThatThrownBy(() -> processor.ingest(records))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("Duplicate incoming NCH_ID 3106");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_missingRequiredField_when_preProcess_then_rejectsRecord() {
        var record = sourceRecord(3802L, 3106L, "Court", 1L);
        record.remove("CourtName");

        assertThatThrownBy(() -> processor.preProcess(List.of(page(record))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("CourtName");
        verifyNoInteractions(tableReadService, bulkUpsertService);
    }

    @Test
    void given_noResolvableId_when_ingest_then_rejectsRecord() {
        var record = sourceRecord(null, null, "Court", 1L);

        assertThatThrownBy(() -> processor.ingest(List.of(page(record))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("NCH_ID");
    }

    @Test
    void given_invalidDate_when_ingest_then_rejectsRecord() {
        var record = sourceRecord(3802L, 3106L, "Court", 1L).put("StartDate", "invalid");

        assertThatThrownBy(() -> processor.ingest(List.of(page(record))))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("StartDate");
    }

    @Test
    void given_recordNodeIsNotObject_when_preProcess_then_preservesItForValidation() {
        var page = OBJECT_MAPPER.createObjectNode();
        page.putArray("records").add("invalid");

        assertThatThrownBy(() -> processor.ingest(List.of(page)))
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("missing expected fields");
    }

    @Test
    void given_emptyInput_when_preProcess_then_returnsEmptyInput() {
        assertThat(processor.preProcess(List.of())).isEmpty();
    }

    private ObjectNode sourceRecord(Long courtId, Long pssId, String name, Long version) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CourtName", name)
                        .putNull("CourtWelshName")
                        .put("CourtLocationCode", "B01CF00")
                        .put("StartDate", "1900-01-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", version);
        record.put("CourtID", courtId);
        record.put("PSSNationalCourtHouseID", pssId);
        return record;
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
