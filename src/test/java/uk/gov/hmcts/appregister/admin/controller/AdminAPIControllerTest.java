package uk.gov.hmcts.appregister.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.admin.service.AdminAPIService;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngestService;
import uk.gov.hmcts.appregister.generated.model.AdminJobStatus;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;

class AdminAPIControllerTest {
    private final AdminAPIService adminAPIService = mock(AdminAPIService.class);
    private final CsdsIngestService csdsIngestService = mock(CsdsIngestService.class);
    private final CsdsIngressProcessor csdsIngressProcessor = mock(CsdsIngressProcessor.class);
    private final AdminAPIController controller =
            new AdminAPIController(adminAPIService, csdsIngestService, csdsIngressProcessor);

    @Test
    void enableDisableDatabaseJobByName_delegatesAndReturnsVersionedOkResponse() {
        ResponseEntity<Void> response =
                controller.enableDisableDatabaseJobByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, true);

        verify(adminAPIService)
                .enableDisableDatabaseJobByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB, true);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getVary()).containsExactly("Accept");
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
    }

    @Test
    void getDatabaseJobRetentionPeriodByName_delegatesAndReturnsBody() {
        var body = new JobRetentionPolicy().retentionPeriodDays(365);
        when(adminAPIService.getDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB))
                .thenReturn(body);

        ResponseEntity<JobRetentionPolicy> response =
                controller.getDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        verify(adminAPIService)
                .getDatabaseJobRetentionPeriodByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    void updateDatabaseJobRetentionPeriodByName_delegatesAndReturnsVersionedOkResponse() {
        ResponseEntity<Void> response =
                controller.updateDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, 365);

        verify(adminAPIService)
                .updateDatabaseJobRetentionPeriodByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB, 365);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getVary()).containsExactly("Accept");
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
    }

    @Test
    void getJobStatus_delegatesAndReturnsBody() {
        var body = new AdminJobStatus().enabled(true);
        when(adminAPIService.getDatabaseJobStatusByName(
                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB))
                .thenReturn(body);

        ResponseEntity<AdminJobStatus> response =
                controller.getJobStatus(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);

        verify(adminAPIService)
                .getDatabaseJobStatusByName(AdminJobType.APPLICATION_LISTS_DATABASE_JOB);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    void ingestCsdsData_delegatesAndReturnsVersionedOkResponse() {
        var file = mock(MultipartFile.class);
        var body = new CsdsIngestResponse().inserted(1).updated(3);
        when(csdsIngestService.ingest(
                        CsdsIngestProcessorName.APPLICATION_CODES.getExternalName(), file))
                .thenReturn(body);

        ResponseEntity<CsdsIngestResponse> response =
                controller.ingestCsdsData(
                        CsdsIngestProcessorName.APPLICATION_CODES.getExternalName(), file);

        verify(csdsIngestService)
                .ingest(CsdsIngestProcessorName.APPLICATION_CODES.getExternalName(), file);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getVary()).containsExactly("Accept");
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    void triggerCsdsIngress_delegatesAndReturnsVersionedOkResponse() {
        ResponseEntity<Void> response = controller.triggerCsdsIngress();

        verify(csdsIngressProcessor).runManualIngress();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getVary()).containsExactly("Accept");
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
    }
}
