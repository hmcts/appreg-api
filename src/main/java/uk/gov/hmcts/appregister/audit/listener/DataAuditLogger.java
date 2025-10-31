package uk.gov.hmcts.appregister.audit.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.appregister.audit.event.AuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditDifferentiable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditDifferentiator;
import uk.gov.hmcts.appregister.audit.listener.diff.Difference;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

/**
 * Manages a data audit logger that writes differences in data for create, update and delete
 * operations to the data audit table.
 */
@Slf4j
@RequiredArgsConstructor
public class DataAuditLogger extends AuditOperationLifecycleListenerAdapter {

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schemaName;

    private final AuditDifferentiator differentiator;

    private final DataAuditRepository dataAuditRepository;

    @Override
    protected void started(StartEvent event) {
        log.info("Starting data audit operation for {}", event);
    }

    @Override
    protected void finished(CompleteEvent event) {
        // data audit for all operations other than read
        if (!event.getRequestAction().getType().isRead()) {
            List<Difference> differenceList = performDifference(event);

            // add each audit record for all identifies differences
            for (Difference difference : differenceList) {
                DataAudit audit = new DataAudit();
                audit.setRelatedKey(getDataId(event));
                audit.setColumnName(difference.getFieldName());
                audit.setNewValue(difference.getNewValue());
                audit.setOldValue(difference.getOldValue());
                audit.setEventName(event.getRequestAction().getEventName());
                audit.setTableName(difference.getTableName());
                audit.setUpdateType(event.getRequestAction().getType());
                audit.setSchemaName(schemaName);

                // save the audit record
                dataAuditRepository.save(audit);
                log.debug("Saving data audit difference: {}", difference);
                log.debug("Saved data audit entity: {}", audit);
            }
        }
    }

    /**
     * gets the id of the new object. If we dont have a new object then return -1
     *
     * @return The data id
     */
    private Long getDataId(CompleteEvent event) {
        Optional<Keyable> val = event.getNewValue();
        return val.isPresent() ? val.get().getId() : -1L;
    }

    @Override
    protected void finishFail(FailEvent event) {
        log.info("Failed data audit operation for {}", event);
    }

    /**
     * establish the differences between old and new values that are stated in the audit.
     *
     * @param event The event
     * @return list of differences
     */
    private List<Difference> performDifference(AuditEvent event) {
        List<Difference> differenceList = new ArrayList<>();
        Optional<Keyable> newKeyable = event.getNewValue();
        Optional<Keyable> oldKeyable = event.getOldValue();
        if (oldKeyable.isPresent()) {
            // if we dont have an object differentiable then use the generic differentiator
            if (newKeyable.isPresent() && newKeyable.get() instanceof AuditDifferentiable diff) {
                Keyable o = oldKeyable.get();
                differenceList = diff.diff(event.getRequestAction().getType(), o);
            } else {
                Keyable newVal = newKeyable.orElse(null);
                Keyable oldVal = oldKeyable.get();

                differenceList =
                        differentiator.diff(event.getRequestAction().getType(), oldVal, newVal);
            }
        } else if (newKeyable.isPresent()) {
            differenceList =
                    differentiator.diff(event.getRequestAction().getType(), newKeyable.get());
        }
        log.debug(
                "Called the audit differentiator and retrieved the differences. Found count: {}",
                differenceList.size());

        return differenceList;
    }
}
