package uk.gov.hmcts.appregister.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.service.ReportJobCreation;
import uk.gov.hmcts.appregister.report.service.ReportService;

class ReportControllerTest {
    private final ReportService reportService = mock(ReportService.class);
    private final JobService jobService = mock(JobService.class);
    private final AuditOperationService auditService = mock(AuditOperationService.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final ReportController controller =
            new ReportController(reportService, jobService, auditService, userProvider);

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        when(userProvider.getUserId()).thenReturn("requesting-user");
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createActivityAuditReport_delegatesToServiceAndReturnsAcceptedResponse() {
        ActivityAuditFilterDto filter = new ActivityAuditFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.ACTIVITY_AUDIT_REPORT);
        when(reportService.createActivityAuditReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response = controller.createActivityAuditReport(filter);

        verify(reportService).createActivityAuditReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createFeesReport_delegatesToServiceAndReturnsAcceptedResponse() {
        FeesReportFilterDto filter = new FeesReportFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.FEES_REPORT);
        when(reportService.createFeesReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response = controller.createFeesReport(filter);

        verify(reportService).createFeesReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createWorkloadReport_delegatesToServiceAndReturnsAcceptedResponse() {
        WorkloadFilterDto filter = new WorkloadFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.WORKLOAD_REPORT);
        when(reportService.createWorkloadReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response = controller.createWorkloadReport(filter);

        verify(reportService).createWorkloadReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createSearchWarrantsReport_delegatesToServiceAndReturnsAcceptedResponse() {
        SearchWarrantsReportFilterDto filter = new SearchWarrantsReportFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.SEARCH_WARRANTS_REPORT);
        when(reportService.createSearchWarrantsReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response = controller.createSearchWarrantsReport(filter);

        verify(reportService).createSearchWarrantsReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createDurationReport_delegatesToServiceAndReturnsAcceptedResponse() {
        DurationFilterDto filter = new DurationFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.DURATION_REPORT);
        when(reportService.createDurationReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response = controller.createDurationReport(filter);

        verify(reportService).createDurationReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createListMaintenanceReport_delegatesToServiceAndReturnsAcceptedResponse() {
        ListMaintenanceFilterDto filter = new ListMaintenanceFilterDto();
        JobAcknowledgement acknowledgement = acknowledgement(JobType.LIST_MAINTENANCE_REPORT);
        when(reportService.createListMaintenanceReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response =
                controller.createListMaintenanceReport(filter);

        verify(reportService).createListMaintenanceReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void createPrivateProsecutorsIndexReport_delegatesToServiceAndReturnsAcceptedResponse() {
        PrivateProsecutorsIndexFilterDto filter = new PrivateProsecutorsIndexFilterDto();
        JobAcknowledgement acknowledgement =
                acknowledgement(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT);
        when(reportService.createPrivateProsecutorsIndexReport(filter))
                .thenReturn(new ReportJobCreation(acknowledgement, null));

        ResponseEntity<JobAcknowledgement> response =
                controller.createPrivateProsecutorsIndexReport(filter);

        verify(reportService).createPrivateProsecutorsIndexReport(filter);
        assertAccepted(response, acknowledgement);
    }

    @Test
    void givenCompletedReportJob_whenDownloadingReport_thenAuditsReportJobDownload()
            throws Exception {
        UUID jobId = UUID.randomUUID();
        JobStatusResponse jobStatusResponse = mock(JobStatusResponse.class);

        when(jobService.getJobStatusById(jobId)).thenReturn(jobStatusResponse);
        when(jobStatusResponse.getStatus()).thenReturn(JobStatus1.COMPLETED);
        when(jobStatusResponse.getUuid()).thenReturn(jobId);
        when(jobStatusResponse.getType()).thenReturn(JobType.FEES_REPORT);
        when(jobStatusResponse.read())
                .thenReturn(
                        new InputStreamResource(
                                new ByteArrayInputStream(
                                        "report".getBytes(StandardCharsets.UTF_8))));
        AtomicReference<Auditable> auditedDownload = new AtomicReference<>();
        runAuditAndCaptureParameters(
                ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT, auditedDownload);

        ResponseEntity<Resource> response = controller.downloadReport(jobId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jobService).getJobStatusById(jobId);

        var auditData = auditedDownload.get().extractAuditData(CrudEnum.READ);
        Assertions.assertTrue(
                auditData.contains(new AuditableData("report_jobs", "jobId", jobId.toString())));
        Assertions.assertTrue(
                auditData.contains(new AuditableData("report_jobs", "reportType", "FEES_REPORT")));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "requestingUser", "requesting-user")));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "fileReference", "report.csv")));
    }

    private void assertAccepted(
            ResponseEntity<JobAcknowledgement> response, JobAcknowledgement acknowledgement) {
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Assertions.assertSame(acknowledgement, response.getBody());
        Assertions.assertEquals(
                "/jobs/" + acknowledgement.getId(), response.getHeaders().getLocation().getPath());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runAuditAndCaptureParameters(
            ReportAuditOperation operation, AtomicReference<Auditable> auditedParameters) {
        doAnswer(
                        invocation -> {
                            Function<BaseAuditEvent, Optional<AuditableResult>> execution =
                                    invocation.getArgument(1);
                            AuditableResult result = execution.apply(null).orElseThrow();
                            auditedParameters.set((Auditable) result.getNewEntity());
                            return result.getResultingValue();
                        })
                .when(auditService)
                .processAudit(eq(operation), any(Function.class));
    }

    private JobAcknowledgement acknowledgement(JobType jobType) {
        return new JobAcknowledgement()
                .id(UUID.randomUUID())
                .type(jobType)
                .status(JobStatus1.RECEIVED);
    }
}
