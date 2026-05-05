package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;

@ExtendWith(MockitoExtension.class)
class AuditedReportLifecycleTest {
    @Mock private AsyncJobLifecycle<String> delegate;
    @Mock private ReportJobAuditService reportJobAuditService;

    @Test
    void givenReportJobCompletes_whenLifecycleRuns_thenAuditsProcessingToCompleted()
            throws IOException {
        AuditedReportLifecycle<String> lifecycle =
                new AuditedReportLifecycle<>(delegate, reportJobAuditService);
        JobStatusResponse response = reportJob(JobType.FEES_REPORT);

        lifecycle.lifeCycleEventPerformed(event(response, new JobContext(), JobStatus1.PROCESSING));
        lifecycle.lifeCycleEventPerformed(event(response, new JobContext(), JobStatus1.COMPLETED));

        verify(delegate, times(2)).lifeCycleEventPerformed(any());
        verify(reportJobAuditService)
                .auditStatusTransition(response, JobStatus1.PROCESSING, JobStatus1.COMPLETED, null);
    }

    @Test
    void givenReportJobCompletesWithoutProcessing_whenLifecycleRuns_thenAuditsReceivedToCompleted()
            throws IOException {
        AuditedReportLifecycle<String> lifecycle =
                new AuditedReportLifecycle<>(delegate, reportJobAuditService);
        JobStatusResponse response = reportJob(JobType.FEES_REPORT);

        lifecycle.lifeCycleEventPerformed(event(response, new JobContext(), JobStatus1.RECEIVED));
        lifecycle.lifeCycleEventPerformed(event(response, new JobContext(), JobStatus1.COMPLETED));

        verify(delegate, times(2)).lifeCycleEventPerformed(any());
        verify(reportJobAuditService)
                .auditStatusTransition(response, JobStatus1.RECEIVED, JobStatus1.COMPLETED, null);
    }

    @Test
    void givenReportJobFails_whenLifecycleRuns_thenAuditsProcessingToFailedWithReason()
            throws IOException {
        AuditedReportLifecycle<String> lifecycle =
                new AuditedReportLifecycle<>(delegate, reportJobAuditService);
        JobStatusResponse response = reportJob(JobType.DURATION_REPORT);
        JobContext context = new JobContext();
        context.logFailure("report failed");

        lifecycle.lifeCycleEventPerformed(event(response, new JobContext(), JobStatus1.PROCESSING));
        lifecycle.lifeCycleEventPerformed(event(response, context, JobStatus1.FAILED));

        verify(reportJobAuditService)
                .auditStatusTransition(
                        response, JobStatus1.PROCESSING, JobStatus1.FAILED, "report failed");
    }

    private AsyncJobLifecycleEvent<String> event(
            JobStatusResponse response, JobContext context, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, List.of(), context, status);
    }

    private JobStatusResponse reportJob(JobType jobType) {
        return JobStatusResponse.builder()
                .uuid(UUID.randomUUID())
                .type(jobType)
                .userName("requesting-user")
                .build();
    }
}
