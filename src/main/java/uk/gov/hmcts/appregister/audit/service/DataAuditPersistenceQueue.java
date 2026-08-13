package uk.gov.hmcts.appregister.audit.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.DataAudit;

/**
 * Queues data-audit writes so callers never wait for a second database connection.
 *
 * <p>The worker invokes {@link DataAuditPersistenceService} outside the caller's transaction. Its
 * {@code REQUIRES_NEW} transaction therefore uses one connection rather than competing with a
 * connection still held by the business transaction.
 *
 * <p>The queue is deliberately bounded and fail-open: when it is full, the audit batch is logged,
 * counted as rejected, and discarded rather than delaying or failing the business request.
 */
@Slf4j
@Component
public class DataAuditPersistenceQueue {
    static final String QUEUE_DEPTH_METRIC = "appreg.audit.persistence.queue.depth";
    static final String SUBMITTED_ROWS_METRIC = "appreg.audit.persistence.rows.submitted";
    static final String REJECTED_ROWS_METRIC = "appreg.audit.persistence.rows.rejected";
    static final String FAILED_ROWS_METRIC = "appreg.audit.persistence.rows.failed";
    static final String DURATION_METRIC = "appreg.audit.persistence.duration";

    private final DataAuditPersistenceService dataAuditPersistenceService;
    private final ThreadPoolExecutor executor;
    private final Duration shutdownTimeout;
    private final Counter submittedRows;
    private final Counter rejectedRows;
    private final Counter failedRows;
    private final Timer persistenceDuration;
    private final AtomicInteger outstandingTasks = new AtomicInteger();

    public DataAuditPersistenceQueue(
            DataAuditPersistenceService dataAuditPersistenceService,
            DataAuditPersistenceProperties properties,
            MeterRegistry meterRegistry) {
        this.dataAuditPersistenceService = dataAuditPersistenceService;
        shutdownTimeout = properties.getShutdownTimeout();
        executor =
                new ThreadPoolExecutor(
                        properties.getWorkerCount(),
                        properties.getWorkerCount(),
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                        Thread.ofPlatform().name("data-audit-", 0).factory(),
                        new ThreadPoolExecutor.AbortPolicy());
        submittedRows = meterRegistry.counter(SUBMITTED_ROWS_METRIC);
        rejectedRows = meterRegistry.counter(REJECTED_ROWS_METRIC);
        failedRows = meterRegistry.counter(FAILED_ROWS_METRIC);
        persistenceDuration = meterRegistry.timer(DURATION_METRIC);
        // Queue capacity is measured in submitted batches; row counters expose their actual sizes.
        Gauge.builder(QUEUE_DEPTH_METRIC, executor.getQueue(), java.util.Collection::size)
                .register(meterRegistry);
    }

    public void submit(List<DataAudit> auditsToPersist) {
        // The caller may clear its transaction-local buffer as soon as submit returns.
        val auditSnapshot = List.copyOf(auditsToPersist);
        // JPA audit listeners and logs still need the originating request identity and trace data.
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        val loggingContext = MDC.getCopyOfContextMap();

        outstandingTasks.incrementAndGet();
        try {
            executor.execute(() -> persist(auditSnapshot, authentication, loggingContext));
            submittedRows.increment(auditSnapshot.size());
        } catch (RejectedExecutionException exception) {
            outstandingTasks.decrementAndGet();
            rejectedRows.increment(auditSnapshot.size());
            log.error(
                    "Data audit persistence queue rejected {} rows; queueDepth={}, activeWorkers={}",
                    auditSnapshot.size(),
                    executor.getQueue().size(),
                    executor.getActiveCount(),
                    exception);
        }
    }

    private void persist(
            List<DataAudit> auditsToPersist,
            Authentication authentication,
            Map<String, String> loggingContext) {
        // Worker threads are reused, so always install and subsequently clear per-request context.
        val securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        setLoggingContext(loggingContext);

        try {
            persistenceDuration.record(() -> dataAuditPersistenceService.persist(auditsToPersist));
            auditsToPersist.forEach(audit -> log.debug("Saved data audit entity: {}", audit));
        } catch (RuntimeException exception) {
            failedRows.increment(auditsToPersist.size());
            auditsToPersist.forEach(
                    audit ->
                            log.error(
                                    "Failed to persist audit field {} on table {}",
                                    audit.getColumnName(),
                                    audit.getTableName(),
                                    exception));
        } finally {
            SecurityContextHolder.clearContext();
            MDC.clear();
            outstandingTasks.decrementAndGet();
        }
    }

    private static void setLoggingContext(Map<String, String> loggingContext) {
        if (loggingContext == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(loggingContext);
        }
    }

    public void awaitIdle(Duration timeout) {
        val deadline = System.nanoTime() + timeout.toNanos();
        while (outstandingTasks.get() > 0) {
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException(
                        "Data audit persistence queue did not become idle within " + timeout);
            }

            try {
                Thread.sleep(10L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while waiting for data audit persistence queue", exception);
            }
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                val abandonedTasks = executor.shutdownNow().size();
                log.error(
                        "Data audit persistence queue did not drain within {}; abandonedTasks={}",
                        shutdownTimeout,
                        abandonedTasks);
            }
        } catch (InterruptedException exception) {
            val abandonedTasks = executor.shutdownNow().size();
            Thread.currentThread().interrupt();
            log.error(
                    "Interrupted while draining data audit persistence queue; abandonedTasks={}",
                    abandonedTasks,
                    exception);
        }
    }
}
