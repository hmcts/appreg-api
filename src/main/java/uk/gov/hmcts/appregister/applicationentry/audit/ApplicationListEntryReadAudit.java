package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.ReflectiveAuditor;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Read-audit wrapper for application list entry searches.
 *
 * <p>The standard reflective auditor does not recurse into collection-valued fields, so joined
 * result-code filters need to be appended explicitly.
 */
@RequiredArgsConstructor
public class ApplicationListEntryReadAudit implements Auditable {

    private final ApplicationListEntry applicationListEntry;
    private final String resulted;

    @Override
    public Long getId() {
        return applicationListEntry.getId();
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        var auditData =
                new ArrayList<>(
                        ReflectiveAuditor.extractAuditData(crudEnum, applicationListEntry, true));

        if (resulted != null && !resulted.isBlank()) {
            auditData.add(
                    new AuditableData(TableNames.RESOLUTION_CODES, "resolution_code", resulted));
        }

        return auditData;
    }
}
