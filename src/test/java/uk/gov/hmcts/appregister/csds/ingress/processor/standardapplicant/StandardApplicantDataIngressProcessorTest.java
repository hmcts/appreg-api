package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditLevel;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class StandardApplicantDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private StandardApplicantIngressApplyService applyService;
    @Mock private CsdsAuditService csdsAuditService;
    @Mock private JdbcIngressBackupService ingressBackupService;

    private CsdsIngressProperties properties;
    private final BusinessDateProvider businessDateProvider =
            new BusinessDateProvider(
                    Clock.fixed(Instant.parse("2026-10-20T23:30:00Z"), ZoneOffset.UTC),
                    ZoneId.of("Europe/London"));
    private StandardApplicantDataIngressProcessor processor;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new CsdsIngressProperties();
        properties.setPageSize(2);
        lenient().when(csdsAuditService.auditLevel()).thenReturn(CsdsAuditLevel.NONE);
        processor =
                new StandardApplicantDataIngressProcessor(
                        businessDateProvider,
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        new StandardApplicantDiffService(
                                tableReadService, new StandardApplicantIngressDatabaseRowMapper()),
                        new StandardApplicantDiffReportingService(properties),
                        applyService);
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
                        "/named-query-count/APPREGISTER/DA_GetStandardApplicant/GD" + parameters))
                .thenReturn(count);
        when(ingressClient.retrieveJson(
                        "/named-query/APPREGISTER/DA_GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=3&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(
                        "/named-query/APPREGISTER/DA_GetStandardApplicant/GD"
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
                        eq("standard_applicants_staging"), eq("sa_id"), diffCaptor.capture());
        assertThat(response.getInserted()).isEqualTo(2);
        assertThat(response.getUpdated()).isZero();
        assertThat(diffCaptor.getValue().incomingById()).containsKeys(9659L, 9660L);
        var fallbackIdRecord = diffCaptor.getValue().incomingById().get(9660L);
        assertThat(fallbackIdRecord.addressLine1()).isEqualTo("<missing>");
        assertThat(fallbackIdRecord.emailAddress()).isEqualTo("email@example.test");
        assertThat(fallbackIdRecord.telephoneNumber()).isEqualTo("020 1234 5678");
    }

    @Test
    void given_reportingDirConfigured_when_apply_then_writesComparisonReports() throws Exception {
        properties.getProcessors().getStandardApplicants().setReportingDir(tempDir.toString());
        processor =
                new StandardApplicantDataIngressProcessor(
                        businessDateProvider,
                        properties,
                        csdsAuditService,
                        passthroughTransactionRunner(),
                        ingressBackupService,
                        new StandardApplicantDiffService(
                                tableReadService, new StandardApplicantIngressDatabaseRowMapper()),
                        new StandardApplicantDiffReportingService(properties),
                        applyService);
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of());

        processor.apply(
                processor.preProcess(
                        List.of(createPage(sourceRecord(9659L, 6278L, "Derbyshire")))));

        try (var files = Files.list(tempDir)) {
            var reports = files.toList();
            for (var report : reports) {
                var name = report.getFileName().toString();
                if (name.endsWith(".csv") && !name.startsWith("standard_applicants_existing_")) {
                    assertThat(Files.readString(report)).contains("\"6278\",\"9659\"");
                    if (report.getFileName().toString().startsWith("standard_applicants_diff_")) {
                        assertThat(Files.readString(report)).contains("\"109659\"");
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
    void preservesLegacyIdsSkipsReplaysAndInsertsNewVersions() {
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(stored(6278L, null, "DCCMH")));
        var diff = diffFor(sourceRecord(1L, 6278L, "New name"));
        assertThat(diff.diffRecords())
                .singleElement()
                .satisfies(
                        change -> {
                            assertThat(change.operation()).isEqualTo(IngressOperation.UPDATE);
                            assertThat(change.intended().id()).isEqualTo(6278L);
                            assertThat(change.intended().csdsId()).isEqualTo(1L);
                        });
        var received = diff.diffRecords().getFirst().intended();
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(received));
        assertThat(diffFor(sourceRecord(1L, 6278L, "Ignored replay")).diffRecords()).isEmpty();
        assertThat(diffFor(sourceRecord(2L, 6278L, "Next version")).diffRecords())
                .singleElement()
                .satisfies(
                        change -> {
                            assertThat(change.operation()).isEqualTo(IngressOperation.INSERT);
                            assertThat(change.intended().id()).isEqualTo(100002L);
                        });
        assertThat(diffFor(sourceRecord(3L, 99L, "Missing legacy")).diffRecords())
                .singleElement()
                .satisfies(change -> assertThat(change.intended().id()).isEqualTo(100003L));
    }

    @Test
    void enrichesUnlinkedOffsetIdWithoutPssIdAndSkipsReplay() {
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(stored(100001L, null, "DCCMH")));

        var diff = diffFor(sourceRecord(1L, null, "Updated"));

        assertThat(diff.diffRecords()).hasSize(1);
        var change = diff.diffRecords().getFirst();
        assertThat(change.operation()).isEqualTo(IngressOperation.UPDATE);
        assertThat(change.intended().id()).isEqualTo(100001L);
        assertThat(change.intended().csdsId()).isEqualTo(1L);
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(change.intended()));
        assertThat(diffFor(sourceRecord(1L, null, "Replay")).diffRecords()).isEmpty();
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(stored(100001L, 2L, "DCCMH")));
        assertThatThrownBy(() -> diffFor(sourceRecord(1L, null, "Different version")))
                .hasMessageContaining("collides");
    }

    @Test
    void duplicateCodesAcrossPagesRemoteIdsAndGeneratedCollisionsFail() {
        var pages =
                processor.preProcess(
                        List.of(
                                createPage(sourceRecord(1L, null, "One")),
                                createPage(sourceRecord(2L, null, "Two"))));
        assertThatThrownBy(() -> processor.diff(pages))
                .hasMessageContaining("Duplicate incoming Code");
        var other = sourceRecord(1L, null, "Other").put("Code", "OTHER");
        assertThatThrownBy(() -> diffFor(sourceRecord(1L, null, "One"), other))
                .hasMessageContaining("Duplicate incoming CSDS_ID");
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(stored(100001L, null, "OTHER")));
        assertThatThrownBy(() -> diffFor(sourceRecord(1L, null, "One")))
                .hasMessageContaining("collides");
    }

    @Test
    void mismatchedLegacyCodeIsNotOverwritten() {
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of(stored(6278L, null, "OTHER")));
        var replacement = sourceRecord(1L, 6278L, "Replacement").put("StartDate", "2025-01-01");
        assertThat(diffFor(replacement).diffRecords())
                .singleElement()
                .satisfies(
                        change -> {
                            assertThat(change.intended().id()).isEqualTo(100001L);
                            assertThat(change.intended().startDate())
                                    .isEqualTo(LocalDate.of(2025, 1, 1));
                        });
    }

    @Test
    void validatesIdentifiersEvenWhenSourceIncludesSaId() throws Exception {
        for (var field : List.of("ApplicantID", "PSSApplicantID")) {
            for (var invalid : List.of("0", "-1", "1.5", "\"123\"", "9223372036854775808")) {
                var node = sourceRecord(1L, 6278L, "One");
                node.set(field, OBJECT_MAPPER.readTree(invalid));
                node.put("SA_ID", 10L);
                assertThatThrownBy(() -> processor.preProcess(List.of(createPage(node))))
                        .isInstanceOf(AppRegistryException.class);
            }
        }
        var node = sourceRecord(2147483648L, null, "One");
        assertThatThrownBy(() -> processor.preProcess(List.of(createPage(node))))
                .isInstanceOf(AppRegistryException.class);
    }

    private StandardApplicantDiffResult diffFor(ObjectNode... records) {
        return processor.diff(processor.preProcess(List.of(createPage(records))));
    }

    private StandardApplicantIngressRecord stored(Long id, Long csdsId, String code) {
        return new StandardApplicantIngressRecord(
                id,
                csdsId,
                code,
                LocalDate.of(2000, 1, 1),
                null,
                0L,
                "Legacy",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void rejectsFutureStartDatesButKeepsTheirCodesForReconciliation() {
        var past = sourceRecord(1L, 1L, "Past").put("StartDate", "2026-10-20");
        var today = sourceRecord(2L, 2L, "Today").put("StartDate", "2026-10-21");
        var future = sourceRecord(3L, 3L, "Future").put("StartDate", "2026-10-22");
        var processed = processor.preProcess(List.of(createPage(past), createPage(today, future)));
        var records = processed.getFirst().get("records");
        assertThat(records.size()).isEqualTo(2);
        assertThat(records.get(0).get("ApplicantID").asLong()).isEqualTo(1L);
        assertThat(records.get(1).get("ApplicantID").asLong()).isEqualTo(2L);
        var rejected = processor.preProcess(List.of(createPage(future))).getFirst();
        assertThat(rejected.get("records").isEmpty()).isTrue();
        assertThat(rejected.get("rejectedFutureCodes").size()).isEqualTo(1);
        assertThat(rejected.get("rejectedFutureCodes").get(0).asText()).isEqualTo("DCCMH");
        when(tableReadService.loadAll(eq("standard_applicants_staging"), any()))
                .thenReturn(List.of());
        assertThat(processor.diff(List.of(rejected)).rejectedFutureCodes())
                .containsExactly("DCCMH");
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
