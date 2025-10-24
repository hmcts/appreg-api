package uk.gov.hmcts.appregister.audit.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.event.AuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditDifferentiator;
import uk.gov.hmcts.appregister.audit.listener.diff.Difference;
import uk.gov.hmcts.appregister.audit.listener.diff.Differentiable;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages a data audit logger that writes differences in data
 * for create, update and delete operations to the data audit table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataAuditLogger extends AuditOperationLifecycleListenerAdapter {
    private final AuditDifferentiator differentiator;

    private DataAuditRepository dataAuditRepository;
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
                audit.setRelatedKey(event instanceof Keyable && event.getNewValue().isPresent()
                        ? event.getNewValue().get().getId() : -1);
                audit.setColumnName(difference.getFieldName());
                audit.setNewValue(difference.getNewValue());
                audit.setOldValue(difference.getOldValue());
                audit.setEventName(event.getRequestAction().getEventName());
                audit.setTableName(difference.getTableName());
                audit.setUpdateType(event.getRequestAction().getType());

                // save the audit record
                dataAuditRepository.save(audit);
            }
        }
    }

    @Override
    protected void finishFail(FailEvent event) {
        log.info("Failed data audit operation for {}", event);
    }

    /**
     * establish the differences between old and new values that are stated in the audit
     * @param event The event
     * @return list of differences
     */
    private List<Difference> performDifference(AuditEvent event) {
        List<Difference> differenceList = new ArrayList<>();

        if (event.getNewValue().isPresent() && event.getOldValue().isPresent()) {
            // check of differentiable is implemented if so use that implementation
            if (differentiator == null  && event.getNewValue().get() instanceof Differentiable diff) {
                Keyable o = event.getNewValue().get();
                differenceList = diff.diff(o);
            }
            else {
                Keyable o = event.getNewValue().get();
                Keyable o1 = event.getOldValue().get();

                differenceList = differentiator.diff(o, o1);
            }
        } else if (event.getNewValue().isPresent()) {
            differenceList = differentiator.diff(event.getNewValue().get());
        }

        return differenceList;
    }
}
