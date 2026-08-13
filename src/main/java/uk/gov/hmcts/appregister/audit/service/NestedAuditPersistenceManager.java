package uk.gov.hmcts.appregister.audit.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.appregister.common.entity.DataAudit;

/**
 * Buffers data audit rows for explicitly nested audit flows and persists them once the outer
 * transaction has completed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NestedAuditPersistenceManager {
    private final DataAuditPersistenceQueue dataAuditPersistenceQueue;

    private final ThreadLocal<State> state = new NamedThreadLocal<>("nested-audit-persistence");

    public void enter() {
        val current = state.get();
        if (current == null) {
            state.set(new State());
            return;
        }

        current.depth++;
    }

    public void exit() {
        val current = state.get();
        if (current == null) {
            return;
        }

        current.depth--;
        if (current.depth > 0) {
            return;
        }

        if (!current.synchronizationRegistered) {
            flushBufferedAudits(current);
            state.remove();
        }
    }

    public void persistOrBuffer(List<DataAudit> auditsToPersist) {
        val current = state.get();
        if (current == null) {
            dataAuditPersistenceQueue.submit(auditsToPersist);
            return;
        }

        current.bufferedAudits.addAll(auditsToPersist);
        registerAfterCompletionFlush(current);
    }

    private void registerAfterCompletionFlush(State current) {
        if (current.synchronizationRegistered
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        // Defer submission until the business transaction has released its database connection.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        val bufferedState = state.get();
                        try {
                            if (bufferedState != null) {
                                flushBufferedAudits(bufferedState);
                            }
                        } finally {
                            state.remove();
                        }
                    }
                });
        current.synchronizationRegistered = true;
    }

    private void flushBufferedAudits(State current) {
        if (current.bufferedAudits.isEmpty()) {
            return;
        }

        try {
            dataAuditPersistenceQueue.submit(current.bufferedAudits);
        } catch (RuntimeException e) {
            logBufferedAuditPersistenceFailure(current.bufferedAudits, e);
        } finally {
            current.bufferedAudits.clear();
        }
    }

    private void logBufferedAuditPersistenceFailure(
            List<DataAudit> bufferedAudits, RuntimeException e) {
        bufferedAudits.forEach(
                audit ->
                        log.error(
                                "Failed to persist buffered audit field {} on table {}",
                                audit.getColumnName(),
                                audit.getTableName(),
                                e));
    }

    private static final class State {
        private int depth = 1;
        private boolean synchronizationRegistered;
        private final List<DataAudit> bufferedAudits = new ArrayList<>();
    }
}
