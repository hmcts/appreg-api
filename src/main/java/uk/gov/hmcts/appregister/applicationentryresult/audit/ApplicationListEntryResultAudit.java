package uk.gov.hmcts.appregister.applicationentryresult.audit;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.ReflectiveAuditor;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Audit wrapper for Application List Entry Result writes.
 *
 * <p>The generic reflective auditor captures scalar fields like wording, officer and version, but
 * it does not emit join-column rows for nested relations. This wrapper keeps the existing
 * reflective behaviour and appends the explicit foreign-key rows the legacy audit contract expects.
 */
@RequiredArgsConstructor(staticName = "from")
public class ApplicationListEntryResultAudit implements Auditable {

    private final AppListEntryResolution resolution;

    @Override
    public Long getId() {
        return resolution.getId();
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        var auditData =
                new ArrayList<>(ReflectiveAuditor.extractAuditData(crudEnum, resolution, true));

        if ((crudEnum == CrudEnum.CREATE || crudEnum == CrudEnum.DELETE)
                && resolution.getApplicationList() != null
                && resolution.getApplicationList().getId() != null) {
            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                            "ale_ale_id",
                            resolution.getApplicationList().getId().toString()));
        }

        if ((crudEnum == CrudEnum.CREATE
                        || crudEnum == CrudEnum.UPDATE
                        || crudEnum == CrudEnum.DELETE)
                && resolution.getResolutionCode() != null
                && resolution.getResolutionCode().getId() != null) {
            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS,
                            "rc_rc_id",
                            resolution.getResolutionCode().getId().toString()));
        }

        return auditData;
    }
}
