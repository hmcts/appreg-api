package uk.gov.hmcts.appregister.audit.listener;

import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;

import java.util.Optional;

class AuditOperationLifecycleListenerAdapterTest {

    @Test
    void testStart() {
        AuditOperationLifecycleListenerAdapter my =
                Mockito.mock(
                        AuditOperationLifecycleListenerAdapter.class, Answers.CALLS_REAL_METHODS);
        my.eventPerformed(new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT, "id", Optional.empty(), Optional.empty()));
        Mockito.verify(my, times(1)).started(Mockito.notNull());
    }

    @Test
    void testComplete() {
        AuditOperationLifecycleListenerAdapter my =
                Mockito.mock(
                        AuditOperationLifecycleListenerAdapter.class, Answers.CALLS_REAL_METHODS);
        my.eventPerformed(
                new CompleteEvent(
                        new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT, "id", Optional.empty(), Optional.empty()),
                        null));
        Mockito.verify(my, times(1)).finished(Mockito.notNull());
    }

    @Test
    void testFail() {
        AuditOperationLifecycleListenerAdapter my =
                Mockito.mock(
                        AuditOperationLifecycleListenerAdapter.class, Answers.CALLS_REAL_METHODS);
        my.eventPerformed(
                new FailEvent(
                        new CompleteEvent(
                                new StartEvent(
                                        AppCodeAuditOperation.GET_APPLICATION_CODE_AUDIT_EVENT, "id", Optional.empty(), Optional.empty()),
                                null)));
        Mockito.verify(my, times(1)).finishFail(Mockito.notNull());
    }
}
