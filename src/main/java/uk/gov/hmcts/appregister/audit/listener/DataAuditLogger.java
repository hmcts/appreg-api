package uk.gov.hmcts.appregister.audit.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.appregister.audit.event.AuditOldNewEnum;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditor;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.audit.service.NestedAuditPersistenceManager;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Manages a data audit logger that writes audit logs for create, update and delete audit operations
 * to the data audit table.
 */
@Slf4j
@RequiredArgsConstructor
public class DataAuditLogger extends AuditOperationLifecycleListenerAdapter {

    private static final int VARCHAR_AUDIT_LIMIT = 4000;

    /** Represents a null value. We default to a null string. */
    public static final String EMPTY_VALUE = "";

    private static final int AUDIT_VARCHAR_LIMIT = 4000;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schemaName;

    private final Auditor auditor;

    private final NestedAuditPersistenceManager nestedAuditPersistenceManager;

    private static final String SAVING_OLD_AUDIT_MESSAGE = "Saving data audit old: {}";
    private static final String SAVING_NEW_AUDIT_MESSAGE = "Saving data audit new: {}";

    @Override
    protected void started(StartEvent event) {
        log.info("Starting data audit operation for {}", event);
    }

    @Override
    protected void finished(CompleteEvent event) {
        validateAuditState(event);

        auditDataBasedOnCompleteEventState(event);
    }

    private static void validateAuditState(CompleteEvent event) {
        val oldValue = event.getOldValue();
        val newValue = event.getNewValue();

        if (oldValue == null && newValue == null) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Cannot audit when both old and new values are null");
        }

        if (oldValue != null
                && newValue != null
                && (oldValue.getClass() != newValue.getClass()
                        || !defaultKeyableId(oldValue).equals(defaultKeyableId(newValue)))) {
            log.debug(
                    "New and old audit values are not the same type and or id {} {}",
                    oldValue.getClass().getCanonicalName(),
                    newValue.getClass().getCanonicalName());
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "New and old audit values are not the same type");
        }
    }

    /**
     * audits data based on the complete audit event state.
     *
     * @param event The event that signifies the auditable operation is complete
     */
    private void auditDataBasedOnCompleteEventState(CompleteEvent event) {

        // determines whether we are auditing old, new or both
        AuditOldNewEnum oldNew = event.getNewOldAuditState();

        switch (oldNew) {
            case OLD -> processOld(event);
            case BOTH -> processBoth(event);
            case NEW -> processNew(event);
        }
    }

    private void processBoth(CompleteEvent event) {
        List<AuditableData> oldDifferenceList = extractAuditData(event.getOldValue(), event);
        List<AuditableData> newDifferenceList = extractAuditData(event.getNewValue(), event);

        boolean oldIsPrimary = oldDifferenceList.size() >= newDifferenceList.size();
        List<AuditableData> primaryList = oldIsPrimary ? oldDifferenceList : newDifferenceList;
        List<AuditableData> secondaryList = oldIsPrimary ? newDifferenceList : oldDifferenceList;

        auditDiff(event, primaryList, secondaryList, oldIsPrimary);
    }

    /**
     * process the auditing for the old data.
     *
     * @param event The completed event.
     */
    private void processOld(CompleteEvent event) {
        List<AuditableData> oldDifferenceList = extractAuditData(event.getOldValue(), event);
        auditDiff(event, oldDifferenceList, null, true);
    }

    /**
     * process the auditing for the new data.
     *
     * @param event The completed event.
     */
    private void processNew(CompleteEvent event) {
        List<AuditableData> newDifferenceList = extractAuditData(event.getNewValue(), event);
        auditDiff(event, newDifferenceList, null, false);
    }

    private List<AuditableData> extractAuditData(Keyable value, CompleteEvent event) {
        if (value instanceof Auditable auditable) {
            return auditable.extractAuditData(event.getRequestAction().getType());
        }

        return auditor.extractAuditData(event.getRequestAction().getType(), value);
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
        val requestAction = event.getRequestAction();
        val updateType = requestAction.getType();
        val eventName = requestAction.getEventName();
        val traceId = AuditOperationServiceImpl.getTraceId();
        val oldRelatedKey = defaultKeyableId(event.getOldValue());
        val newRelatedKey = defaultKeyableId(event.getNewValue());
        val secondaryByField = indexAuditData(secondaryList);

        List<DataAudit> auditsToPersist = new ArrayList<>(primaryList.size());

        for (val diff : primaryList) {
            val secondaryDiff = getCorrespondingData(diff, secondaryByField);
            if (shouldSkipAuditPersistence(updateType, diff, secondaryDiff, primaryOld)) {
                logAuditValues(diff, secondaryDiff, primaryOld);
                continue;
            }

            val audit = new DataAudit();
            audit.setColumnName(diff.getFieldName());
            audit.setEventName(eventName);
            audit.setTableName(diff.getTableName());
            audit.setUpdateType(updateType);
            audit.setSchemaName(schemaName);
            audit.setLink(traceId);
            logAuditValues(diff, secondaryDiff, primaryOld);
            setNewAndOldAuditValues(
                    audit, diff, secondaryDiff, primaryOld, oldRelatedKey, newRelatedKey);
            auditsToPersist.add(audit);
        }

        if (auditsToPersist.isEmpty()) {
            return;
        }

        try {
            nestedAuditPersistenceManager.persistOrBuffer(auditsToPersist);
        } catch (RuntimeException e) {
            logAuditPersistenceFailure(auditsToPersist, e);
            val firstAudit = auditsToPersist.getFirst();
            throw new RuntimeException(
                    "Failed to persist audit field %s on table %s"
                            .formatted(firstAudit.getColumnName(), firstAudit.getTableName()),
                    e);
        }
    }

    private static boolean shouldSkipAuditPersistence(
            CrudEnum updateType,
            AuditableData primaryDiff,
            AuditableData secondaryDiff,
            boolean primaryOld) {
        return updateType == CrudEnum.READ
                && EMPTY_VALUE.equals(resolveNewValue(primaryDiff, secondaryDiff, primaryOld));
    }

    private static void logAuditPersistenceFailure(
            List<DataAudit> auditsToPersist, RuntimeException e) {
        auditsToPersist.forEach(
                audit ->
                        log.error(
                                "Failed to persist audit field {} on table {}",
                                audit.getColumnName(),
                                audit.getTableName(),
                                e));
    }

    /**
     * Indexes audit data by table and field name for constant-time lookup.
     *
     * @param auditData The audit data to index
     * @return audit data keyed by table and field name
     */
    private static Map<AuditFieldKey, AuditableData> indexAuditData(List<AuditableData> auditData) {
        if (auditData == null || auditData.isEmpty()) {
            return Map.of();
        }

        Map<AuditFieldKey, AuditableData> auditDataByField = HashMap.newHashMap(auditData.size());
        for (val diff : auditData) {
            auditDataByField.put(new AuditFieldKey(diff.getTableName(), diff.getFieldName()), diff);
        }
        return auditDataByField;
    }

    private static AuditableData getCorrespondingData(
            AuditableData dataToFind, Map<AuditFieldKey, AuditableData> secondaryByField) {
        return secondaryByField.get(
                new AuditFieldKey(dataToFind.getTableName(), dataToFind.getFieldName()));
    }

    /**
     * Sets the new and old audit values on the data audit record based on the event state.
     *
     * @param audit The data audit record
     * @param primaryDiff The primary audit data
     * @param secondaryDiff The secondary audit data
     */
    private static void setNewAndOldAuditValues(
            DataAudit audit,
            AuditableData primaryDiff,
            AuditableData secondaryDiff,
            boolean primaryOld,
            Long oldRelatedKey,
            Long newRelatedKey) {
        if (primaryOld && secondaryDiff == null) {
            audit.setRelatedKey(oldRelatedKey);
            setNewAuditValue(audit, EMPTY_VALUE);
            setOldAuditValue(audit, primaryDiff.getValue());
        } else if (!primaryOld && secondaryDiff == null) {
            audit.setRelatedKey(newRelatedKey);
            setNewAuditValue(audit, primaryDiff.getValue());
            setOldAuditValue(audit, EMPTY_VALUE);
        } else {
            audit.setRelatedKey(newRelatedKey);

            if (primaryOld) {
                setOldAuditValue(audit, primaryDiff.getValue());
                setNewAuditValue(audit, secondaryDiff.getValue());
            } else {
                setOldAuditValue(audit, secondaryDiff.getValue());
                setNewAuditValue(audit, primaryDiff.getValue());
            }
        }
    }

    private static void setOldAuditValue(DataAudit audit, String value) {
        if (shouldUseClob(value)) {
            audit.setOldValue(null);
            audit.setOldClobValue(value);
            return;
        }

        audit.setOldValue(value);
        audit.setOldClobValue(null);
    }

    private static void setNewAuditValue(DataAudit audit, String value) {
        if (shouldUseClob(value)) {
            audit.setNewValue(null);
            audit.setNewClobValue(value);
            return;
        }

        audit.setNewValue(value);
        audit.setNewClobValue(null);
    }

    private static boolean shouldUseClob(String value) {
        return value != null && value.length() > AUDIT_VARCHAR_LIMIT;
    }

    private static void logAuditValues(
            AuditableData primaryDiff, AuditableData secondaryDiff, boolean primaryOld) {
        if (primaryOld && secondaryDiff == null) {
            log.debug(SAVING_OLD_AUDIT_MESSAGE, primaryDiff);
        } else if (!primaryOld && secondaryDiff == null) {
            log.debug(SAVING_NEW_AUDIT_MESSAGE, primaryDiff);
        } else if (primaryOld) {
            log.debug(SAVING_OLD_AUDIT_MESSAGE, primaryDiff);
            log.debug(SAVING_NEW_AUDIT_MESSAGE, secondaryDiff);
        } else {
            log.debug(SAVING_NEW_AUDIT_MESSAGE, primaryDiff);
            log.debug(SAVING_OLD_AUDIT_MESSAGE, secondaryDiff);
        }
    }

    private static String resolveNewValue(
            AuditableData primaryDiff, AuditableData secondaryDiff, boolean primaryOld) {
        if (secondaryDiff == null) {
            return primaryOld ? EMPTY_VALUE : primaryDiff.getValue();
        }

        return primaryOld ? secondaryDiff.getValue() : primaryDiff.getValue();
    }

    /**
     * The default keyable long with a default.
     *
     * @param keyable The long or a default value if null
     * @return The long or -1 if null
     */
    private static Long defaultKeyableId(Keyable keyable) {
        if (keyable != null && keyable.getId() != null) {
            return keyable.getId();
        }

        return -1L;
    }

    @Override
    protected void finishFail(FailEvent event) {
        log.info("Business operation failed for audited event {}", event);
    }

    private record AuditFieldKey(String tableName, String fieldName) {}
}
