package uk.gov.hmcts.appregister.audit.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.appregister.audit.listener.DataAuditLogger;
import uk.gov.hmcts.appregister.common.entity.DataAudit;

/**
 * Buffers data audit rows for explicitly nested audit flows and persists them once the outer
 * transaction has completed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NestedAuditPersistenceManager {
    private static final Logger DATA_AUDIT_LOGGER = LoggerFactory.getLogger(DataAuditLogger.class);

    private final DataAuditPersistenceService dataAuditPersistenceService;

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
            dataAuditPersistenceService.persist(auditsToPersist);
            auditsToPersist.forEach(
                    audit -> DATA_AUDIT_LOGGER.debug("Saved data audit entity: {}", audit));
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
            dataAuditPersistenceService.persist(current.bufferedAudits);
            current.bufferedAudits.forEach(
                    audit -> DATA_AUDIT_LOGGER.debug("Saved data audit entity: {}", audit));
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
