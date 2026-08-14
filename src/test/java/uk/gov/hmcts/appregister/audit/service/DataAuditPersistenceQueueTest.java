package uk.gov.hmcts.appregister.audit.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@ExtendWith(MockitoExtension.class)
class DataAuditPersistenceQueueTest {
    @Mock private DataAuditPersistenceService dataAuditPersistenceService;

    private SimpleMeterRegistry meterRegistry;
    private DataAuditPersistenceQueue persistenceQueue;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var properties = new DataAuditPersistenceProperties();
        properties.setWorkerCount(1);
        properties.setQueueCapacity(1);
        properties.setShutdownTimeout(Duration.ofSeconds(1));
        persistenceQueue =
                new DataAuditPersistenceQueue(
                        dataAuditPersistenceService, properties, meterRegistry);
    }

    @AfterEach
    void tearDown() {
        persistenceQueue.shutdown();
        meterRegistry.close();
    }

    @Test
    void givenAuditRows_whenSubmitted_thenSnapshotIsPersistedAsynchronously() throws Exception {
        var persisted = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            persisted.countDown();
                            return null;
                        })
                .when(dataAuditPersistenceService)
                .persist(anyList());
        var audits = new java.util.ArrayList<>(List.of(audit("first"), audit("second")));

        persistenceQueue.submit(audits);
        audits.clear();

        Assertions.assertTrue(persisted.await(1, SECONDS));
        verify(dataAuditPersistenceService)
                .persist(Mockito.argThat(persistedRows -> persistedRows.size() == 2));
        Assertions.assertEquals(
                2, meterRegistry.counter(DataAuditPersistenceQueue.SUBMITTED_ROWS_METRIC).count());
    }

    @Test
    void givenWorkerAndQueueAreBusy_whenAnotherBatchIsSubmitted_thenItIsRejectedWithoutBlocking()
            throws Exception {
        var workerStarted = new CountDownLatch(1);
        var releaseWorker = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            workerStarted.countDown();
                            releaseWorker.await(1, SECONDS);
                            return null;
                        })
                .when(dataAuditPersistenceService)
                .persist(anyList());

        persistenceQueue.submit(List.of(audit("active")));
        Assertions.assertTrue(workerStarted.await(1, SECONDS));
        persistenceQueue.submit(List.of(audit("queued")));

        Assertions.assertTimeoutPreemptively(
                Duration.ofMillis(250), () -> persistenceQueue.submit(List.of(audit("rejected"))));

        Assertions.assertEquals(
                1, meterRegistry.counter(DataAuditPersistenceQueue.REJECTED_ROWS_METRIC).count());
        releaseWorker.countDown();
        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> verify(dataAuditPersistenceService, times(2)).persist(anyList()));
    }

    @Test
    void givenPersistenceFails_whenWorkerRuns_thenFailureIsRecordedAndSuppressed() {
        var logCaptor = LogCaptor.forClass(DataAuditPersistenceQueue.class);
        doThrow(new RuntimeException("database unavailable"))
                .when(dataAuditPersistenceService)
                .persist(anyList());

        persistenceQueue.submit(List.of(audit("failed")));

        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            Assertions.assertEquals(
                                    1,
                                    meterRegistry
                                            .counter(DataAuditPersistenceQueue.FAILED_ROWS_METRIC)
                                            .count());
                            Assertions.assertTrue(
                                    logCaptor.getErrorLogs().stream()
                                            .anyMatch(
                                                    log ->
                                                            log.contains(
                                                                    "Failed to persist audit field failed")));
                        });
    }

    @Test
    void givenShutdownTimeoutIsNotPositive_whenValidated_thenItIsInvalid() {
        var properties = new DataAuditPersistenceProperties();
        Assertions.assertTrue(properties.isShutdownTimeoutValid());

        properties.setShutdownTimeout(Duration.ZERO);
        Assertions.assertFalse(properties.isShutdownTimeoutValid());

        properties.setShutdownTimeout(null);
        Assertions.assertFalse(properties.isShutdownTimeoutValid());
    }

    @Test
    void givenPersistenceIsStillRunning_whenAwaitingIdleTimesOut_thenFailureIsReported()
            throws Exception {
        var workerStarted = new CountDownLatch(1);
        var releaseWorker = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            workerStarted.countDown();
                            releaseWorker.await(1, SECONDS);
                            return null;
                        })
                .when(dataAuditPersistenceService)
                .persist(anyList());
        persistenceQueue.submit(List.of(audit("active")));
        Assertions.assertTrue(workerStarted.await(1, SECONDS));

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> persistenceQueue.awaitIdle(Duration.ofMillis(20)));

        releaseWorker.countDown();
        Assertions.assertDoesNotThrow(() -> persistenceQueue.awaitIdle(Duration.ofSeconds(1)));
    }

    private static DataAudit audit(String columnName) {
        var audit = new DataAudit();
        audit.setColumnName(columnName);
        audit.setTableName(TableNames.APPLICATION_CODES);
        return audit;
    }
}
