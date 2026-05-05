package uk.gov.hmcts.appregister.report.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

@ExtendWith(MockitoExtension.class)
class ReportJobAuditServiceTest {
    @Mock private AuditOperationService auditService;

    @Test
    void givenReportJobCompletesFromProcessing_whenAuditingTransition_thenWritesUpdateAudit() {
        ReportJobAuditService service = new ReportJobAuditService(auditService, List.of());
        JobStatusResponse job = reportJob(JobType.FEES_REPORT);
        ArgumentCaptor<ReportJobAudit> oldAuditCaptor =
                ArgumentCaptor.forClass(ReportJobAudit.class);
        ArgumentCaptor<Function<BaseAuditEvent, Optional<AuditableResult<Object, ReportJobAudit>>>>
                executionCaptor = ArgumentCaptor.forClass(Function.class);

        Mockito.doReturn(null)
                .when(auditService)
                .processAudit(
                        oldAuditCaptor.capture(),
                        eq(ReportAuditOperation.REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT),
                        executionCaptor.capture(),
                        any(AuditOperationLifecycleListener[].class));

        service.auditStatusTransition(job, JobStatus1.PROCESSING, JobStatus1.COMPLETED, null);

        // Execute the captured callback to inspect the new audit payload that would be persisted.
        ReportJobAudit oldAudit = oldAuditCaptor.getValue();
        ReportJobAudit newAudit =
                executionCaptor.getValue().apply(null).orElseThrow().getNewEntity();

        Assertions.assertTrue(
                oldAudit.extractAuditData(CrudEnum.UPDATE).contains(status(JobStatus1.PROCESSING)));
        Assertions.assertTrue(
                newAudit.extractAuditData(CrudEnum.UPDATE).contains(status(JobStatus1.COMPLETED)));
        Assertions.assertTrue(
                newAudit.extractAuditData(CrudEnum.UPDATE)
                        .contains(
                                new AuditableData(
                                        "report_jobs",
                                        "reportType",
                                        JobType.FEES_REPORT.toString())));
    }

    @Test
    void givenReportJobFailsFromProcessing_whenAuditingTransition_thenIncludesErrorReason() {
        ReportJobAuditService service = new ReportJobAuditService(auditService, List.of());
        JobStatusResponse job = reportJob(JobType.DURATION_REPORT);
        ArgumentCaptor<Function<BaseAuditEvent, Optional<AuditableResult<Object, ReportJobAudit>>>>
                executionCaptor = ArgumentCaptor.forClass(Function.class);

        Mockito.doReturn(null)
                .when(auditService)
                .processAudit(
                        any(ReportJobAudit.class),
                        eq(ReportAuditOperation.REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT),
                        executionCaptor.capture(),
                        any(AuditOperationLifecycleListener[].class));

        service.auditStatusTransition(
                job, JobStatus1.PROCESSING, JobStatus1.FAILED, "report failed");

        // Execute the captured callback to inspect the new audit payload that would be persisted.
        ReportJobAudit newAudit =
                executionCaptor.getValue().apply(null).orElseThrow().getNewEntity();

        Assertions.assertTrue(
                newAudit.extractAuditData(CrudEnum.UPDATE).contains(status(JobStatus1.FAILED)));
        Assertions.assertTrue(
                newAudit.extractAuditData(CrudEnum.UPDATE)
                        .contains(
                                new AuditableData("report_jobs", "errorReason", "report failed")));
    }

    @Test
    void givenReportJobFailsWithoutReason_whenAuditingTransition_thenIncludesFallbackReason() {
        ReportJobAuditService service = new ReportJobAuditService(auditService, List.of());
        JobStatusResponse job = reportJob(JobType.DURATION_REPORT);
        ArgumentCaptor<Function<BaseAuditEvent, Optional<AuditableResult<Object, ReportJobAudit>>>>
                executionCaptor = ArgumentCaptor.forClass(Function.class);

        Mockito.doReturn(null)
                .when(auditService)
                .processAudit(
                        any(ReportJobAudit.class),
                        eq(ReportAuditOperation.REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT),
                        executionCaptor.capture(),
                        any(AuditOperationLifecycleListener[].class));

        service.auditStatusTransition(job, JobStatus1.PROCESSING, JobStatus1.FAILED, null);

        ReportJobAudit newAudit =
                executionCaptor.getValue().apply(null).orElseThrow().getNewEntity();

        Assertions.assertTrue(
                newAudit.extractAuditData(CrudEnum.UPDATE)
                        .contains(
                                new AuditableData(
                                        "report_jobs",
                                        "errorReason",
                                        "Failed with unknown error")));
    }

    @Test
    void givenNonReportJobOrIntermediateTransition_whenAuditingTransition_thenSkipsAudit() {
        ReportJobAuditService service = new ReportJobAuditService(auditService, List.of());

        service.auditStatusTransition(
                reportJob(JobType.BULK_UPLOAD_ENTRIES),
                JobStatus1.PROCESSING,
                JobStatus1.COMPLETED,
                null);
        service.auditStatusTransition(
                reportJob(JobType.FEES_REPORT), JobStatus1.VALIDATING, JobStatus1.PROCESSING, null);

        verify(auditService, never())
                .processAudit(
                        any(ReportJobAudit.class),
                        any(),
                        any(),
                        any(AuditOperationLifecycleListener[].class));
    }

    private JobStatusResponse reportJob(JobType jobType) {
        return JobStatusResponse.builder()
                .uuid(UUID.randomUUID())
                .type(jobType)
                .userName("requesting-user")
                .build();
    }

    private AuditableData status(JobStatus1 status) {
        return new AuditableData("report_jobs", "status", status.toString());
    }
}
