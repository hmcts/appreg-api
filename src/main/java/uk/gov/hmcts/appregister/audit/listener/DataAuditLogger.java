package uk.gov.hmcts.appregister.audit.listener;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.appregister.audit.event.AuditOldNewEnum;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditor;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Manages a data audit logger that writes audit logs for create, update and delete audit operations
 * to the data audit table.
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

    private static final String SAVING_OLD_AUDIT_MESSAGE = "Saving data audit old: {}";
    private static final String SAVING_NEW_AUDIT_MESSAGE = "Saving data audit new: {}";

    @Override
    protected void started(StartEvent event) {
        log.info("Starting data audit operation for {}", event);
    }

    @Override
    protected void finished(CompleteEvent event) {
        // make sure if we are comparing old or new then the types match
        if (event.getOldValue() != null
                && event.getNewValue() != null
                && ((!event.getOldValue()
                                .getClass()
                                .getCanonicalName()
                                .equals(event.getNewValue().getClass().getCanonicalName()))
                        || !event.getOldValue().getId().equals(event.getNewValue().getId()))) {
            log.debug(
                    "New and old audit values are not the same type and or id{} {}",
                    event.getOldValue().getClass().getCanonicalName(),
                    event.getNewValue().getClass().getCanonicalName());
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "New and old audit values are not the same type");
        } else if (event.getOldValue() == null && event.getNewValue() == null) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot audit when both old and new values are null");
        }

        auditDataBasedOnCompleteEventState(event);
    }

    /**
     * audits data based on the complete audit event state.
     *
     * @param event The event that signifies the auditable operation is complete
     */
    private void auditDataBasedOnCompleteEventState(CompleteEvent event) {

        // determines whether we are auditing old, new or both
        AuditOldNewEnum oldNew = event.getNewOldAuditState();

        // if we just have old gather the old data difference
        if (oldNew == AuditOldNewEnum.OLD) {
            processOld(event);
            // if both new and old values are present
        } else if (oldNew == AuditOldNewEnum.BOTH) {
            List<AuditableData> oldDifferenceList;
            List<AuditableData> newDifferenceList;
            if (event.getOldValue() instanceof Auditable auditDifferentiable) {
                oldDifferenceList =
                        auditDifferentiable.extractAuditData(event.getRequestAction().getType());
            } else {
                oldDifferenceList =
                        auditor.extractAuditData(
                                event.getRequestAction().getType(), event.getOldValue());
            }

            if (event.getNewValue() instanceof Auditable newAuditDifferentiable) {
                newDifferenceList =
                        newAuditDifferentiable.extractAuditData(event.getRequestAction().getType());
            } else {
                newDifferenceList =
                        auditor.extractAuditData(
                                event.getRequestAction().getType(), event.getNewValue());
            }

            // based on the largest size we define the primary and secondary lists to compare
            List<AuditableData> primaryList =
                    oldDifferenceList.size() >= newDifferenceList.size()
                            ? oldDifferenceList
                            : newDifferenceList;
            List<AuditableData> secondaryList =
                    oldDifferenceList.size() >= newDifferenceList.size()
                            ? newDifferenceList
                            : oldDifferenceList;

            auditDiff(
                    event,
                    primaryList,
                    secondaryList,
                    oldDifferenceList.size() >= newDifferenceList.size());
        } else {
            processNew(event);
        }
    }

    /**
     * process the auditing for the old data.
     *
     * @param event The completed event.
     */
    private void processOld(CompleteEvent event) {
        List<AuditableData> oldDifferenceList;

        // prioritise the extract audit data from Auditable if implemented
        if (event.getOldValue() instanceof Auditable auditDifferentiable) {
            oldDifferenceList =
                    auditDifferentiable.extractAuditData(event.getRequestAction().getType());
        } else {
            oldDifferenceList =
                    auditor.extractAuditData(
                            event.getRequestAction().getType(), event.getOldValue());
        }

        auditDiff(event, oldDifferenceList, null, true);
    }

    /**
     * process the auditing for the new data.
     *
     * @param event The completed event.
     */
    private void processNew(CompleteEvent event) {
        List<AuditableData> newDifferenceList;
        if (event.getNewValue() instanceof Auditable auditDifferentiable) {
            newDifferenceList =
                    auditDifferentiable.extractAuditData(event.getRequestAction().getType());
        } else {
            newDifferenceList =
                    auditor.extractAuditData(
                            event.getRequestAction().getType(), event.getNewValue());
        }

        auditDiff(event, newDifferenceList, null, false);
    }

    /**
     * Audits the auditable data found between old and new data.
     *
     * @param event The event that has occured
     * @param primaryList The primary list either old if we have an old only audit, or new if we
     *     have new only audit,
     * @param secondaryList The secondary list
     * @param primaryOld Whether the primary list is old
     */
    private void auditDiff(
            CompleteEvent event,
            List<AuditableData> primaryList,
            List<AuditableData> secondaryList,
            boolean primaryOld) {
        for (int i = 0; i < primaryList.size(); i++) {
            AuditableData diff = primaryList.get(i);
            DataAudit audit = new DataAudit();
            audit.setColumnName(diff.getFieldName());

            audit.setEventName(event.getRequestAction().getEventName());
            audit.setTableName(diff.getTableName());
            audit.setUpdateType(event.getRequestAction().getType());
            audit.setSchemaName(schemaName);

            // store the new and old values based on the state
            setNewAndOldAuditValues(
                    audit, diff, getCorrespondingData(diff, secondaryList), event, primaryOld);

            // save the audit record
            dataAuditRepository.save(audit);

            log.debug("Saved data audit entity: {}", audit);
        }
    }

    /**
     * finds the new value update for the old auditable data.
     *
     * @param dataToFind The auditable data to find the new value for
     * @param listToFindData The list to find the data in
     * @return the equivalent new auditable data to find for the value
     */
    private AuditableData getCorrespondingData(
            AuditableData dataToFind, List<AuditableData> listToFindData) {
        if (listToFindData != null) {
            for (AuditableData diff : listToFindData) {
                if (diff.getTableName().equals(dataToFind.getTableName())
                        && diff.getFieldName().equals(dataToFind.getFieldName())) {
                    return diff;
                }
            }
        }

        return null;
    }

    /**
     * Sets the new and old audit values on the data audit record based on the event state.
     *
     * @param audit The data audit record
     * @param primaryDiff The primary audit data
     * @param secondaryDiff The secondary audit data
     * @param event The complete event
     */
    private void setNewAndOldAuditValues(
            DataAudit audit,
            AuditableData primaryDiff,
            AuditableData secondaryDiff,
            CompleteEvent event,
            boolean primaryOld) {
        if (primaryOld && secondaryDiff == null) {
            log.debug(SAVING_OLD_AUDIT_MESSAGE, primaryDiff);
            audit.setRelatedKey(event.getOldValue() != null ? event.getOldValue().getId() : -1L);
            audit.setNewValue(EMPTY_VALUE);
            audit.setOldValue(primaryDiff.getValue());
        } else if (!primaryOld && secondaryDiff == null) {
            log.debug(SAVING_NEW_AUDIT_MESSAGE, primaryDiff);
            audit.setRelatedKey(event.getNewValue() != null ? event.getNewValue().getId() : -1L);
            audit.setNewValue(primaryDiff.getValue());
            audit.setOldValue(EMPTY_VALUE);
        } else {
            audit.setRelatedKey(event.getOldValue() != null ? event.getNewValue().getId() : -1L);

            if (primaryOld) {
                audit.setOldValue(primaryDiff.getValue());
                audit.setNewValue(secondaryDiff.getValue());
                log.debug(SAVING_OLD_AUDIT_MESSAGE, primaryDiff);
                log.debug(SAVING_NEW_AUDIT_MESSAGE, secondaryDiff);
            } else {
                audit.setOldValue(secondaryDiff.getValue());
                audit.setNewValue(primaryDiff.getValue());
                log.debug(SAVING_NEW_AUDIT_MESSAGE, primaryDiff);
                log.debug(SAVING_OLD_AUDIT_MESSAGE, secondaryDiff);
            }
        }
    }

    @Override
    protected void finishFail(FailEvent event) {
        log.info("Failed data audit operation for {}", event);
    }
}
