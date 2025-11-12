package uk.gov.hmcts.appregister.audit.listener;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.appregister.audit.event.*;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditor;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Manages a data audit logger that writes audit logs for create, update and delete
 * audit operations to the data audit table.
 */
@Slf4j
@RequiredArgsConstructor
public class DataAuditLogger extends AuditOperationLifecycleListenerAdapter {

    /** Represents a null value. We default to a null string. */
    public static final String EMPTY_VALUE = "";

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schemaName;

    private final Auditor auditor;

    private final DataAuditRepository dataAuditRepository;

    @Override
    protected void started(StartEvent event) {
        log.info("Starting data audit operation for {}", event);
    }

    @Override
    protected void finished(CompleteEvent event) {
        // make sure if we are comparing old or new then the types match
        if (event.getOldValue() != null
                && event.getNewValue() != null
                && !event.getOldValue().getClass().getCanonicalName()
                .equals(event.getNewValue().getClass().getCanonicalName())) {
            log.debug("ENew and old audit values are not the same type {} {}",
                    event.getOldValue().getClass().getCanonicalName(),
                    event.getNewValue().getClass().getCanonicalName());
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "New and old audit values are not the same type");
        } else if (event.getOldValue() == null
                && event.getNewValue() == null) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot audit when both old and new values are null");
        }

        // data audit for all operations. Ignores get operations
        if (!event.getRequestAction().getType().isRead()) {
            AuditOldNewEnum oldNew = event.getNewOldAuditState();

            if (oldNew == AuditOldNewEnum.OLD) {
                List<AuditableData> oldDifferenceList;
                if (event.getOldValue() instanceof Auditable auditDifferentiable) {
                    oldDifferenceList = auditDifferentiable.extractAuditData(event.getRequestAction().getType());
                } else {
                    oldDifferenceList = auditor.extractAuditData(event.getRequestAction().getType(), event.getOldValue());
                }

                auditDiff(event, oldDifferenceList, null);
            } else if (oldNew == AuditOldNewEnum.BOTH) {
                List<AuditableData> oldDifferenceList;
                List<AuditableData> newDifferenceList;
                if (event.getOldValue() instanceof Auditable auditDifferentiable) {
                    oldDifferenceList = auditDifferentiable.extractAuditData(event.getRequestAction().getType());
                }
                else {
                    oldDifferenceList = auditor.extractAuditData(event.getRequestAction().getType(), event.getOldValue());
                }

                if (event.getNewValue() instanceof Auditable newAuditDifferentiable) {
                    newDifferenceList = newAuditDifferentiable.extractAuditData(event.getRequestAction().getType());
                } else {
                    newDifferenceList = auditor.extractAuditData(event.getRequestAction().getType(), event.getNewValue());
                }

                auditDiff(event, oldDifferenceList, newDifferenceList);
            } else {
                List<AuditableData> newDifferenceList;
                if (event.getNewValue() instanceof Auditable auditDifferentiable) {
                    newDifferenceList = auditDifferentiable.extractAuditData(event.getRequestAction().getType());
                } else {
                    newDifferenceList = auditor.extractAuditData(event.getRequestAction().getType(), event.getNewValue());
                }

                auditDiff(event, newDifferenceList, null);
            }
        }
    }

    /**
     * Audits the auditable data found between old and new data.
     * @param event The event that has occured
     * @param primaryList The primary list either old if we have an old only audit, or new if we have new only audit,
     * @param newDifferenceData Always represents the new audit data. Used if we have both old and new auditable
     *                          data to audit, else will be null.
     */
    private void auditDiff(CompleteEvent event, List<AuditableData> primaryList,
                           List<AuditableData> newDifferenceData) {
            for (int i = 0; i < primaryList.size(); i++) {
                AuditableData diff = primaryList.get(i);
                DataAudit audit = new DataAudit();
                audit.setColumnName(diff.getFieldName());

                audit.setEventName(event.getRequestAction().getEventName());
                audit.setTableName(diff.getTableName());
                audit.setUpdateType(event.getRequestAction().getType());
                audit.setSchemaName(schemaName);

                // store the new and old values based on the state
                setNewAndOldAuditValues(audit, diff, newDifferenceData == null ? null :
                        newDifferenceData.get(i), event);

                // save the audit record
                dataAuditRepository.save(audit);

                log.debug("Saved data audit entity: {}", audit);
            }
        }

        /**
         * Sets the new and old audit values on the data audit record based on the event state.
         * @param audit The data audit record
         * @param primaryDiff The primary audit data
         * @param secondaryDiff The secondary audit data
         * @param event The complete event
         */
        private void setNewAndOldAuditValues(DataAudit audit, AuditableData primaryDiff,
                                        AuditableData secondaryDiff,
                                    CompleteEvent event) {
            if (event.getNewOldAuditState() == AuditOldNewEnum.OLD) {
                audit.setRelatedKey(event.getOldValue() != null ? event.getOldValue().getId() : -1L);
                audit.setNewValue(EMPTY_VALUE);
                audit.setOldValue(primaryDiff.getValue());
                log.debug("Saving data audit old: {}", primaryDiff);
            } else if  (event.getNewOldAuditState() == AuditOldNewEnum.NEW) {
                audit.setRelatedKey(event.getNewValue() != null ? event.getNewValue().getId() : -1L);
                audit.setNewValue(primaryDiff.getValue());
                audit.setOldValue(EMPTY_VALUE);
                log.debug("Saving data audit new: {}", primaryDiff);
            } else {
                audit.setRelatedKey(event.getOldValue() != null ? event.getNewValue().getId() : -1L);
                audit.setNewValue(secondaryDiff.getValue());
                audit.setOldValue(primaryDiff.getValue());
                log.debug("Saving data audit old: {}", primaryDiff);
                log.debug("Saving data audit new: {}", secondaryDiff);
            }
        }

    @Override
    protected void finishFail(FailEvent event) {
        log.info("Failed data audit operation for {}", event);
    }
}
