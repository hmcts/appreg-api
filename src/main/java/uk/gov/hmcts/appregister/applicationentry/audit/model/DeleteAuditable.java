package uk.gov.hmcts.appregister.applicationentry.audit.model;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * This is an auditable class that allows us to audit log for the application list entry delete
 * operation.
 */
public class DeleteAuditable implements Auditable {
    private ApplicationListEntry applicationListEntry;

    public DeleteAuditable(ApplicationListEntry applicationListEntry) {
        this.applicationListEntry = applicationListEntry;
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        List<AuditableData> auditData = new ArrayList<>();

        // we only audit for delete operations
        if (crudEnum.isDelete()) {
            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LISTS_ENTRY,
                            "id",
                            applicationListEntry.getId().toString()));

            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LISTS_ENTRY,
                            "al_id",
                            applicationListEntry.getApplicationList().getId().toString()));

            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LISTS_ENTRY,
                            "version",
                            applicationListEntry.getApplicationList().getVersion().toString()));

            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LISTS_ENTRY,
                            "a_na_id",
                            applicationListEntry.getAnamedaddress().getId().toString()));

            auditData.add(
                    new AuditableData(
                            TableNames.APPLICATION_LISTS_ENTRY,
                            "r_na_id",
                            applicationListEntry.getRnameaddress().getId().toString()));

            // audit the resolutions
            for (int i = 0; i < applicationListEntry.getResolutions().size(); i++) {
                auditData.add(
                        new AuditableData(
                                TableNames.APPLICATION_LISTS_ENTRY,
                                "aler_id" + i,
                                applicationListEntry.getResolutions().get(i).toString()));
            }
        } else {
            throw new UnsupportedOperationException("Unsupported operation");
        }

        return auditData;
    }
}
