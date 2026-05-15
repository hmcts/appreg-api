package uk.gov.hmcts.appregister.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ActivityAuditReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.DurationReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.FeesReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ListMaintenanceReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.SearchWarrantsReportParameterAudit;
import uk.gov.hmcts.appregister.report.service.ReportJobCreation;
import uk.gov.hmcts.appregister.report.service.ReportService;

class ReportControllerTest {
    private final ReportService reportService = mock(ReportService.class);
    private final JobService jobService = mock(JobService.class);
    private final AuditOperationService auditService = mock(AuditOperationService.class);
    private final UserProvider userProvider = mock(UserProvider.class);
    private final ReportController controller =
            new ReportController(reportService, jobService, auditService, List.of(), userProvider);

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
    void givenActivityAuditDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.ACTIVITY_AUDIT_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1))
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));
        ActivityAuditFilterDto normalisedFilter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));

        when(reportService.createActivityAuditReport(filter))
                .thenReturn(
                        new ReportJobCreation(
                                acknowledgement,
                                ActivityAuditReportParameterAudit.from(normalisedFilter)));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT, auditedParameters);

        controller.createActivityAuditReport(filter);

        verify(reportService).createActivityAuditReport(filter);
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(
                                new AuditableData("report_parameters", "dateFrom", "2018-05-01")));
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(new AuditableData("report_parameters", "dateTo", "2018-05-31")));
        assertReportJobAudit(auditedParameters.get(), acknowledgement);
    }

    @Test
    void givenFeesDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.FEES_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));
        FeesReportFilterDto normalisedFilter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));

        when(reportService.createFeesReport(filter))
                .thenReturn(
                        new ReportJobCreation(
                                acknowledgement, FeesReportParameterAudit.from(normalisedFilter)));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, auditedParameters);

        controller.createFeesReport(filter);

        verify(reportService).createFeesReport(filter);
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(
                                new AuditableData("report_parameters", "dateFrom", "2018-05-01")));
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(new AuditableData("report_parameters", "dateTo", "2018-05-31")));
        assertReportJobAudit(auditedParameters.get(), acknowledgement);
    }

    @Test
    void givenSearchWarrantsDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.SEARCH_WARRANTS_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        SearchWarrantsReportFilterDto filter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));
        SearchWarrantsReportFilterDto normalisedFilter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));

        when(reportService.createSearchWarrantsReport(filter))
                .thenReturn(
                        new ReportJobCreation(
                                acknowledgement,
                                SearchWarrantsReportParameterAudit.from(normalisedFilter)));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT, auditedParameters);

        controller.createSearchWarrantsReport(filter);

        verify(reportService).createSearchWarrantsReport(filter);
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(
                                new AuditableData("report_parameters", "dateFrom", "2018-05-01")));
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(new AuditableData("report_parameters", "dateTo", "2018-05-31")));
        assertReportJobAudit(auditedParameters.get(), acknowledgement);
    }

    @Test
    void givenDurationDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.DURATION_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));
        DurationFilterDto normalisedFilter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));

        when(reportService.createDurationReport(filter))
                .thenReturn(
                        new ReportJobCreation(
                                acknowledgement,
                                DurationReportParameterAudit.from(normalisedFilter)));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, auditedParameters);

        controller.createDurationReport(filter);

        verify(reportService).createDurationReport(filter);
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(
                                new AuditableData("report_parameters", "dateFrom", "2018-05-01")));
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(new AuditableData("report_parameters", "dateTo", "2018-05-31")));
        assertReportJobAudit(auditedParameters.get(), acknowledgement);
    }

    @Test
    void givenListMaintenanceDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.LIST_MAINTENANCE_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        ListMaintenanceFilterDto filter =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));
        ListMaintenanceFilterDto normalisedFilter =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31));

        when(reportService.createListMaintenanceReport(filter))
                .thenReturn(
                        new ReportJobCreation(
                                acknowledgement,
                                ListMaintenanceReportParameterAudit.from(normalisedFilter)));
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT, auditedParameters);

        controller.createListMaintenanceReport(filter);

        verify(reportService).createListMaintenanceReport(filter);
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(
                                new AuditableData("report_parameters", "dateFrom", "2018-05-01")));
        Assertions.assertTrue(
                auditedParameters
                        .get()
                        .extractAuditData(CrudEnum.READ)
                        .contains(new AuditableData("report_parameters", "dateTo", "2018-05-31")));
        assertReportJobAudit(auditedParameters.get(), acknowledgement);
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

        controller.downloadReport(jobId);

        List<AuditableData> auditData = auditedDownload.get().extractAuditData(CrudEnum.READ);
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
                .processAudit(
                        eq(operation),
                        any(Function.class),
                        any(AuditOperationLifecycleListener[].class));
    }

    private JobAcknowledgement acknowledgement(JobType jobType) {
        return new JobAcknowledgement()
                .id(UUID.randomUUID())
                .type(jobType)
                .status(JobStatus1.RECEIVED);
    }

    private void assertReportJobAudit(Auditable auditedParameters, JobAcknowledgement job) {
        List<AuditableData> auditData = auditedParameters.extractAuditData(CrudEnum.CREATE);
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "jobId", job.getId().toString())));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "reportType", job.getType().toString())));
        Assertions.assertTrue(
                auditData.contains(
                        new AuditableData("report_jobs", "requestingUser", "requesting-user")));
    }
}
