package uk.gov.hmcts.appregister.report.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
import uk.gov.hmcts.appregister.report.service.ReportDownload;
import uk.gov.hmcts.appregister.report.service.ReportJobCreation;
import uk.gov.hmcts.appregister.report.service.ReportService;

class ReportControllerTest {
    private final ReportService reportService = mock(ReportService.class);
    private final ReportController controller = new ReportController(reportService);

    @BeforeEach
    void setUpRequestContext() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
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
    void downloadReport_delegatesToServiceAndReturnsOkResponse() {
        UUID jobId = UUID.randomUUID();
        var resource =
                new InputStreamResource(
                        new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));
        when(reportService.downloadReport(jobId))
                .thenReturn(new ReportDownload("report.csv", resource));

        ResponseEntity<Resource> response = controller.downloadReport(jobId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertSame(resource, response.getBody());
        Assertions.assertEquals(
                "attachment; filename=\"report.csv\"",
                response.getHeaders().getFirst("Content-Disposition"));
        verify(reportService).downloadReport(jobId);
    }

    private void assertAccepted(
            ResponseEntity<JobAcknowledgement> response, JobAcknowledgement acknowledgement) {
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Assertions.assertSame(acknowledgement, response.getBody());
        Assertions.assertEquals(
                "/jobs/" + acknowledgement.getId(), response.getHeaders().getLocation().getPath());
    }

    private JobAcknowledgement acknowledgement(JobType jobType) {
        return new JobAcknowledgement()
                .id(UUID.randomUUID())
                .type(jobType)
                .status(JobStatus1.RECEIVED);
    }
}
