package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.List;
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
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressTableReadService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class StandardApplicantDataIngressProcessorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private CsdsIngressClient ingressClient;
    @Mock private JdbcIngressTableReadService tableReadService;
    @Mock private StandardApplicantIngressApplyService applyService;
    @Mock private CsdsAuditService csdsAuditService;

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
                        new StandardApplicantDiffService(
                                tableReadService, new StandardApplicantIngressDatabaseRowMapper()),
                        new StandardApplicantDiffReportingService(properties),
                        applyService);
    }

    @Test
    void given_standardApplicantParameters_when_retrieve_then_usesNamedQueryPath() {
        properties
                .getProcessors()
                .getStandardApplicants()
                .setParameters("?$f=PublishingStatus='Active'");
        var firstPage = createPage(OBJECT_MAPPER.createObjectNode());
        var secondPage = createPage(OBJECT_MAPPER.createObjectNode());
        var emptyPage = createPage();
        var parameters = "?$f=PublishingStatus='Active'";

        when(ingressClient.retrieveJson(
                        "/named-query/CSDS/DA_GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=2&%24offset=0"))
                .thenReturn(firstPage);
        when(ingressClient.retrieveJson(
                        "/named-query/CSDS/DA_GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=2&%24offset=2"))
                .thenReturn(secondPage);
        when(ingressClient.retrieveJson(
                        "/named-query/CSDS/DA_GetStandardApplicant/GD"
                                + parameters
                                + "&%24limit=2&%24offset=4"))
                .thenReturn(emptyPage);

        assertThat(processor.retrieve(ingressClient)).containsExactly(firstPage, secondPage);
    }

    @Test
    void given_incomingApplicants_when_ingest_then_reconcilesAndUpsertsConfiguredStagingTable() {
        var withPssId = sourceRecord(9659L, 6278L, "Derbyshire County Council");
        var withoutPssId = sourceRecord(9660L, null, "No address applicant");
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
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .anyMatch(name -> name.startsWith("standard_applicants_incoming_"))
                    .anyMatch(name -> name.startsWith("standard_applicants_existing_"))
                    .anyMatch(name -> name.startsWith("standard_applicants_diff_"));
        }
    }

    private ObjectNode sourceRecord(Long applicantId, Long psssaId, String organisationName) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("ApplicantID", applicantId)
                        .put("Code", "DCCMH")
                        .put("OrganisationName", organisationName)
                        .put("StartDate", "2018-08-01")
                        .putNull("Enddate")
                        .put("RevisionNumber", 2);
        if (psssaId == null) {
            record.putNull("PSSSAID");
        } else {
            record.put("PSSSAID", psssaId);
        }
        record.putArray("Address").addObject().put("AddressLine1", "County Hall");
        record.putArray("ContactInformation")
                .addObject()
                .put("ContactType", "Email Address")
                .put("ContactValue", "email@example.test");
        record.withArray("ContactInformation")
                .addObject()
                .put("ContactType", "Telephone")
                .put("ContactValue", "020 1234 5678");
        return record;
    }

    private ObjectNode createPage(JsonNode... records) {
        var page = OBJECT_MAPPER.createObjectNode();
        var array = page.putArray("records");
        for (var record : records) {
            array.add(record);
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
