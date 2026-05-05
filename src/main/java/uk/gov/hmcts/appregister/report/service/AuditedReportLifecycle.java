package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycle;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.report.audit.ReportJobAuditService;

/**
 * Report-only lifecycle decorator that emits audit rows for terminal report job transitions.
 *
 * <p>The shared async framework publishes lifecycle events for all async jobs. Keeping this wrapper
 * in the report package lets reporting audit those events without making the generic async
 * persistence layer report-aware.
 */
class AuditedReportLifecycle<T> implements AsyncJobLifecycle<T> {
    private final AsyncJobLifecycle<T> delegate;
    private final ReportJobAuditService reportJobAuditService;
    private JobStatus1 previousStatus;

    AuditedReportLifecycle(
            AsyncJobLifecycle<T> delegate, ReportJobAuditService reportJobAuditService) {
        this.delegate = delegate;
        this.reportJobAuditService = reportJobAuditService;
    }

    @Override
    public void lifeCycleEventPerformed(AsyncJobLifecycleEvent<T> lifecycleEvent)
            throws IOException {
        JobStatus1 status = lifecycleEvent.getJobStatus();

        try {
            delegate.lifeCycleEventPerformed(lifecycleEvent);
            auditTransition(lifecycleEvent, status);
        } finally {
            previousStatus = status;
        }
    }

    private void auditTransition(AsyncJobLifecycleEvent<T> lifecycleEvent, JobStatus1 status) {
        if (status != JobStatus1.COMPLETED && status != JobStatus1.FAILED) {
            return;
        }

        if (lifecycleEvent.getResponse() == null) {
            return;
        }

        reportJobAuditService.auditStatusTransition(
                lifecycleEvent.getResponse(),
                previousStatus,
                status,
                failureReason(lifecycleEvent));
    }

    private String failureReason(AsyncJobLifecycleEvent<T> lifecycleEvent) {
        if (lifecycleEvent.getJobStatus() != JobStatus1.FAILED
                || lifecycleEvent.getContext() == null
                || !lifecycleEvent.getContext().hasFailure()) {
            return null;
        }

        return lifecycleEvent.getContext().getCommaDelimitedFailureMessage();
    }
}
