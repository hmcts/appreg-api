package uk.gov.hmcts.appregister.report.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

/**
 * Auditable representation of a report job lifecycle event.
 *
 * <p>The existing audit framework persists one row per returned {@link AuditableData} item, so this
 * model flattens the report job details that ARCPOC-1225 requires into field/value audit entries.
 */
@Builder
@Getter
public class ReportJobAudit implements Auditable {
    private static final String TABLE_NAME = "report_jobs";

    private final UUID jobId;
    private final JobType reportType;
    private final String requestingUser;
    private final JobStatus1 status;
    private final String errorReason;
    private final String fileReference;
    private final Auditable reportParameters;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> data = new ArrayList<>();
        add(data, "jobId", jobId);
        add(data, "reportType", reportType);
        add(data, "requestingUser", requestingUser);
        add(data, "status", status);
        add(data, "errorReason", errorReason);
        add(data, "fileReference", fileReference);

        if (reportParameters != null) {
            data.addAll(reportParameters.extractAuditData(crudEnum));
        }

        return data;
    }

    /**
     * Builds the create-audit payload from the job acknowledgement returned to the caller.
     *
     * <p>Report parameter audit data is preserved so the existing creation audit evidence remains
     * available alongside the new jobId/reportType/requestingUser fields.
     */
    public static ReportJobAudit created(
            JobAcknowledgement acknowledgement, String requestingUser, Auditable reportParameters) {
        return ReportJobAudit.builder()
                .jobId(acknowledgement.getId())
                .reportType(acknowledgement.getType())
                .requestingUser(requestingUser)
                .reportParameters(reportParameters)
                .build();
    }

    /** Builds one side of a status transition audit comparison. */
    public static ReportJobAudit transition(
            UUID jobId,
            JobType reportType,
            String requestingUser,
            JobStatus1 status,
            String errorReason) {
        return ReportJobAudit.builder()
                .jobId(jobId)
                .reportType(reportType)
                .requestingUser(requestingUser)
                .status(status)
                .errorReason(errorReason)
                .build();
    }

    private void add(List<AuditableData> data, String name, Object value) {
        if (value != null) {
            data.add(new AuditableData(TABLE_NAME, name, value.toString()));
        }
    }
}
