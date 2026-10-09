package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.ZoneOffset;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class StandardApplicantDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T23:30:00Z"), ZoneOffset.UTC);

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private StandardApplicantIngressApplyService applyService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    private CsdsIngressProperties properties;
    private StandardApplicantDataIngressProcessor processor;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        processor =
                new StandardApplicantDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        new StandardApplicantDiffService(
                                tableReadService, new StandardApplicantIngressDatabaseRowMapper()),
                        new StandardApplicantDiffReportingService(properties),
                        applyService,
                        CLOCK);
    }

    @Test
    void given_standardApplicantParameters_when_retrieve_then_usesNamedQueryCountAndQueryPaths() {
        properties
                .getProcessors()
                .getStandardApplicants()
                .setParameters("?$f=PublishingStatus='Active'");
        var firstPage = createPage(OBJECT_MAPPER.createObjectNode());
        firstPage.withArray("records").addObject();
        var secondPage = createPage(OBJECT_MAPPER.createObjectNode());
        var count = OBJECT_MAPPER.createObjectNode().put("count", 3);
        var parameters = "?$f=PublishingStatus='Active'";

        when(ingressClient.retrieveJson(
                        "/named-query-count/APPREGISTER/GetStandardApplicant/GD" + parameters))
                .thenReturn(count);
        when(ingressClient.retrieveJson(
                        "/named-query/APPREGISTER/GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(
                        "/named-query/APPREGISTER/GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=1&%24offset=2"))
                .thenReturn(secondPage);

        assertThat(processor.retrieve(ingressClient)).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_incomingApplicants_when_ingest_then_reconcilesAndUpsertsConfiguredStagingTable() {
        final var withPssId = sourceRecord(9659L, 6278L, "Derbyshire County Council");
        var withoutPssId = sourceRecord(9660L, null, "No address applicant");
        withoutPssId.put("Code", "OTHER");
        withoutPssId.putArray("Address");
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of());

        var response = processor.ingest(List.of(createPage(withPssId, withoutPssId)));

        var diffCaptor = ArgumentCaptor.forClass(StandardApplicantDiffResult.class);
        verify(applyService)
                .reconcileAndUpsert(
                        eq("standard_applicants_staging"),
                        eq(List.of("standard_applicant_code")),
                        diffCaptor.capture());
        assertThat(response.getInserted()).isEqualTo(2);
        assertThat(response.getUpdated()).isZero();
        assertThat(diffCaptor.getValue().incomingById()).containsKeys(6278L, 109660L);
        var fallbackIdRecord = diffCaptor.getValue().incomingById().get(109660L);
        assertThat(fallbackIdRecord.addressLine1()).isEqualTo("<missing>");
        assertThat(fallbackIdRecord.emailAddress()).isEqualTo("email@example.test");
        assertThat(fallbackIdRecord.telephoneNumber()).isEqualTo("020 1234 5678");
    }

    @Test
    void given_reportingDirConfigured_when_apply_then_writesComparisonReports() throws Exception {
        properties.getProcessors().getStandardApplicants().setReportingDir(tempDir.toString());
        processor =
                new StandardApplicantDataIngressProcessor(
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        new StandardApplicantDiffService(
                                tableReadService, new StandardApplicantIngressDatabaseRowMapper()),
                        new StandardApplicantDiffReportingService(properties),
                        applyService,
                        CLOCK);
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(existing(42L, "DCCMH")));

        processor.apply(
                processor.preProcess(
                        List.of(createPage(sourceRecord(9659L, 6278L, "Derbyshire")))));

        try (var files = Files.list(tempDir)) {
            var reports = files.toList();
            for (var report : reports) {
                var name = report.getFileName().toString();
                if (name.endsWith(".csv") && !name.startsWith("standard_applicants_existing_")) {
                    assertThat(Files.readString(report)).contains("\"6278\",\"9659\"");
                    if (name.startsWith("standard_applicants_diff_")) {
                        assertThat(Files.readString(report)).contains("\"6278\",\"9659\",\"42\"");
                    }
                }
            }
            assertThat(reports.stream().map(path -> path.getFileName().toString()).toList())
                    .anyMatch(name -> name.startsWith("standard_applicants_incoming_"))
                    .anyMatch(name -> name.startsWith("standard_applicants_existing_"))
                    .anyMatch(name -> name.startsWith("standard_applicants_diff_"));
        }
    }

    @Test
    void existingCodeRetainsStoredIdWithChangedMissingAndOverflowingSourceIds() {
        when(tableReadService.loadAll(any(), any())).thenReturn(List.of(existing(42L, "DCCMH")));
        for (var source :
                List.of(
                        sourceRecord(9659L, 6278L, "Changed"),
                        sourceRecord(Long.MAX_VALUE, null, "Overflow source"),
                        sourceRecord(1L, null, "Missing"))) {
            if (source.path("OrganisationName").asText().equals("Missing")) {
                source.remove(List.of("ApplicantID", "PSSApplicantID"));
            }
            var diff = processor.diff(processor.preProcess(List.of(createPage(source))));
            assertThat(diff.incomingById()).containsOnlyKeys(42L);
            assertThat(diff.incomingById().get(42L).name())
                    .isEqualTo(source.path("OrganisationName").asText());
            assertThat(diff.diffRecords().getFirst().existing().id()).isEqualTo(42L);
        }
    }

    @Test
    void duplicateCodesAcrossPagesFailBeforeWrites() {
        var first = sourceRecord(1L, null, "First");
        var second = sourceRecord(2L, null, "Second");
        assertThatThrownBy(() -> processor.ingest(List.of(createPage(first), createPage(second))))
                .hasMessageContaining("Duplicate incoming standard_applicant_code");
        verifyNoInteractions(applyService);
    }

    @Test
    void newRecordsWithoutIdsOverflowOrCollideFailBeforeWrites() {
        var missing = sourceRecord(1L, null, "Missing");
        missing.remove(List.of("ApplicantID", "PSSApplicantID"));
        assertThatThrownBy(() -> processor.ingest(List.of(createPage(missing))))
                .hasMessageContaining("Missing source ID");
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(
                                                createPage(
                                                        sourceRecord(
                                                                Long.MAX_VALUE,
                                                                null,
                                                                "Overflow")))))
                .isInstanceOf(ArithmeticException.class);

        when(tableReadService.loadAll(any(), any()))
                .thenReturn(List.of(existing(100001L, "STORED")));
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(createPage(sourceRecord(1L, null, "Collision")))))
                .hasMessageContaining("Conflicting SA_ID");
        when(tableReadService.loadAll(any(), any())).thenReturn(List.of());
        var differentCode = sourceRecord(2L, 100001L, "Incoming collision").put("Code", "OTHER");
        assertThatThrownBy(
                        () ->
                                processor.ingest(
                                        List.of(
                                                createPage(
                                                        sourceRecord(1L, null, "Fallback"),
                                                        differentCode))))
                .hasMessageContaining("Conflicting SA_ID");
        verifyNoInteractions(applyService);
    }

    @Test
    void futureRecordsAreWarnedAndDroppedBeforeIdentityAndDuplicatesAtUkMidnight() {
        var today = sourceRecord(1L, null, "Today").put("StartDate", "2026-07-02");
        var future = sourceRecord(Long.MAX_VALUE, null, "Future").put("StartDate", "2026-07-03");
        try (var logs = LogCaptor.forClass(StandardApplicantDataIngressProcessor.class)) {
            var processed = processor.preProcess(List.of(createPage(today), createPage(future)));
            assertThat(processed.getFirst().path("records")).hasSize(1);
            assertThat(processor.diff(processed).incomingById()).containsOnlyKeys(100001L);
            assertThat(logs.getWarnLogs())
                    .anyMatch(
                            log ->
                                    log.contains("DCCMH")
                                            && log.contains("2026-07-03")
                                            && log.contains("today 2026-07-02"));
            var allFuture = processor.diff(processor.preProcess(List.of(createPage(future))));
            assertThat(allFuture.incomingById()).isEmpty();
            assertThat(allFuture.diffRecords()).isEmpty();
        }
        assertThat(
                        processor
                                .preProcess(
                                        List.of(
                                                createPage(
                                                        today.deepCopy()
                                                                .put("StartDate", "2020-01-01")
                                                                .put("EndDate", "2020-12-31"))))
                                .getFirst()
                                .path("records"))
                .hasSize(1);
        assertThat(today.has("SA_ID")).isFalse();
    }

    @Test
    void invalidDatesOnLaterPagesFailIncludingFutureRecords() {
        var valid = sourceRecord(1L, null, "Valid");
        for (var start : List.of("", "not-a-date", "2026-02-30")) {
            var invalid = valid.deepCopy().put("StartDate", start);
            assertThatThrownBy(
                            () ->
                                    processor.preProcess(
                                            List.of(createPage(valid), createPage(invalid))))
                    .isInstanceOf(RuntimeException.class);
        }
        for (var invalid : List.of(valid.deepCopy().putNull("StartDate"), valid.deepCopy())) {
            if (!invalid.path("StartDate").isNull()) {
                invalid.remove("StartDate");
            }
            assertThatThrownBy(() -> processor.preProcess(List.of(createPage(invalid))))
                    .isInstanceOf(RuntimeException.class);
        }
        for (var end : List.of("", "not-a-date", "2026-02-30")) {
            var invalid = valid.deepCopy().put("StartDate", "2026-07-03").put("EndDate", end);
            assertThatThrownBy(
                            () -> processor.ingest(List.of(createPage(valid), createPage(invalid))))
                    .isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(applyService);
    }

    private StandardApplicantIngressRecord existing(Long id, String code) {
        return new StandardApplicantIngressRecord(
                id,
                code,
                LocalDate.parse("2018-08-01"),
                null,
                2L,
                "Stored",
                "County Hall",
                null,
                null,
                null,
                null,
                null,
                "email@example.test",
                "020 1234 5678");
    }

    private ObjectNode sourceRecord(
            Long applicantId, Long pssApplicantId, String organisationName) {
        var sourceRecord =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("ApplicantID", applicantId)
                        .put("Code", "DCCMH")
                        .put("OrganisationName", organisationName)
                        .put("StartDate", "2018-08-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", 2);
        if (pssApplicantId == null) {
            sourceRecord.putNull("PSSApplicantID");
        } else {
            sourceRecord.put("PSSApplicantID", pssApplicantId);
        }
        sourceRecord.putArray("Address").addObject().put("AddressLine1", "County Hall");
        sourceRecord
                .putArray("ContactInformation")
                .addObject()
                .put("ContactType", "Email Address")
                .put("ContactValue", "email@example.test");
        sourceRecord
                .withArray("ContactInformation")
                .addObject()
                .put("ContactType", "Telephone")
                .put("ContactValue", "020 1234 5678");
        return sourceRecord;
    }

    private ObjectNode createPage(JsonNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var array = page.putArray("records");
        for (var sourceRecord : records) {
            array.add(sourceRecord);
        }
        return page;
    }

    private CsdsIngressTransactionRunner passthroughTransactionRunner() {
        return new CsdsIngressTransactionRunner() {
            @Override
            public <T> T execute(java.util.function.Supplier<T> supplier) {
                return supplier.get();
            }
        };
    }
}
