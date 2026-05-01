package uk.gov.hmcts.appregister.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.service.ReportFilterNormaliser;
import uk.gov.hmcts.appregister.report.service.ReportService;

class ReportControllerTest {
    private final ReportService reportService = mock(ReportService.class);
    private final JobService jobService = mock(JobService.class);
    private final JobMapper jobMapper = mock(JobMapper.class);
    private final AuditOperationService auditService = mock(AuditOperationService.class);
    private final ReportController controller =
            new ReportController(
                    reportService,
                    jobService,
                    jobMapper,
                    auditService,
                    List.of(),
                    new ReportFilterNormaliser());

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
    void givenActivityAuditDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.ACTIVITY_AUDIT_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        when(reportService.createActivityAuditReport(any())).thenReturn(acknowledgement);
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT, auditedParameters);

        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1))
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));

        controller.createActivityAuditReport(filter);

        verify(reportService)
                .createActivityAuditReport(
                        org.mockito.ArgumentMatchers.argThat(
                                normalisedFilter ->
                                        LocalDate.of(2018, 5, 1)
                                                        .equals(normalisedFilter.getDateFrom())
                                                && LocalDate.of(2018, 5, 31)
                                                        .equals(normalisedFilter.getDateTo())));
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
    }

    @Test
    void givenFeesDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.FEES_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        when(reportService.createFeesReport(any())).thenReturn(acknowledgement);
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, auditedParameters);

        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));

        controller.createFeesReport(filter);

        verify(reportService)
                .createFeesReport(
                        org.mockito.ArgumentMatchers.argThat(
                                normalisedFilter ->
                                        LocalDate.of(2018, 5, 1)
                                                        .equals(normalisedFilter.getDateFrom())
                                                && LocalDate.of(2018, 5, 31)
                                                        .equals(normalisedFilter.getDateTo())));
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
    }

    @Test
    void givenDurationDatesAreReversed_whenCreatingReport_thenAuditsNormalisedDates() {
        JobAcknowledgement acknowledgement = acknowledgement(JobType.DURATION_REPORT);
        AtomicReference<Auditable> auditedParameters = new AtomicReference<>();

        when(reportService.createDurationReport(any())).thenReturn(acknowledgement);
        runAuditAndCaptureParameters(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, auditedParameters);

        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));

        controller.createDurationReport(filter);

        verify(reportService)
                .createDurationReport(
                        org.mockito.ArgumentMatchers.argThat(
                                normalisedFilter ->
                                        LocalDate.of(2018, 5, 1)
                                                        .equals(normalisedFilter.getDateFrom())
                                                && LocalDate.of(2018, 5, 31)
                                                        .equals(normalisedFilter.getDateTo())));
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
}
