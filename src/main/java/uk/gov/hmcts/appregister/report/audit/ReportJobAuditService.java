package uk.gov.hmcts.appregister.report.audit;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * Emits report job lifecycle audit records for the terminal transitions required by ARCPOC-1225.
 *
 * <p>The async job framework is shared by reports and non-report jobs, so this service owns the
 * filtering rules that keep the new audit rows scoped to report jobs only.
 */
@Component
@RequiredArgsConstructor
public class ReportJobAuditService {
    private static final String UNKNOWN_ERROR = "Failed with unknown error";

    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    /**
     * Audits report job completion or failure when the job transitions out of PROCESSING.
     *
     * <p>Audit listener failures are handled by {@link AuditOperationService}, preserving the
     * primary job status update.
     */
    public void auditStatusTransition(
            JobStatusResponse jobStatusResponse,
            JobStatus1 previousStatus,
            JobStatus1 newStatus,
            String errorReason) {
        if (!shouldAudit(jobStatusResponse, previousStatus, newStatus, errorReason)) {
            return;
        }

        ReportJobAudit oldAudit =
                ReportJobAudit.transition(
                        jobStatusResponse.getUuid(),
                        jobStatusResponse.getType(),
                        jobStatusResponse.getUserName(),
                        previousStatus,
                        null);
        ReportJobAudit newAudit =
                ReportJobAudit.transition(
                        jobStatusResponse.getUuid(),
                        jobStatusResponse.getType(),
                        jobStatusResponse.getUserName(),
                        newStatus,
                        failureReason(newStatus, errorReason));

        auditService.processAudit(
                oldAudit,
                ReportAuditOperation.REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT,
                unused -> Optional.of(new AuditableResult<>(null, newAudit)),
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    /** Applies the ticket scope: report jobs only, and only PROCESSING to COMPLETED or FAILED. */
    private boolean shouldAudit(
            JobStatusResponse jobStatusResponse,
            JobStatus1 previousStatus,
            JobStatus1 newStatus,
            String errorReason) {
        if (jobStatusResponse == null
                || jobStatusResponse.getType() == null
                || jobStatusResponse.getUuid() == null) {
            return false;
        }

        if (!jobStatusResponse.getType().getValue().endsWith("_REPORT")) {
            return false;
        }

        if (previousStatus != JobStatus1.PROCESSING) {
            return false;
        }

        return newStatus == JobStatus1.COMPLETED || newStatus == JobStatus1.FAILED;
    }

    private String failureReason(JobStatus1 newStatus, String errorReason) {
        if (newStatus != JobStatus1.FAILED) {
            return null;
        }

        return errorReason == null ? UNKNOWN_ERROR : errorReason;
    }
}
