package uk.gov.hmcts.appregister.common.audit.listener;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.common.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.common.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.common.audit.event.FailEvent;
import uk.gov.hmcts.appregister.common.audit.event.StartEvent;

/**
 * An abstract class that breaks down the {@link AuditOperationLifecycleListener} into seperate well
 * defined methods.
 */
@Slf4j
public abstract class AuditOperationLifecycleListenerAdapter
        implements AuditOperationLifecycleListener {

    @Override
    public void eventPerformed(BaseAuditEvent event) {
        if (event instanceof StartEvent startEvent) {
            started(startEvent);
        } else if (event instanceof CompleteEvent completeEvent) {
            finished(completeEvent);
        } else if (event instanceof FailEvent failEvent) {
            finishFail(failEvent);
        }
    }

    /**
     * audit before operation has started.
     *
     * @param event The request for the audit
     */
    protected abstract void started(StartEvent event);

    /**
     * audit when operation has finished.
     *
     * @param event The request for the audit
     */
    protected abstract void finished(CompleteEvent event);

    /**
     * audit when operation has failed.
     *
     * @param event The request for the audit
     */
    protected abstract void finishFail(FailEvent event);
}
