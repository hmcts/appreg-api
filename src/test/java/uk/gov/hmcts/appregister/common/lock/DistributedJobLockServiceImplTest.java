package uk.gov.hmcts.appregister.common.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@ExtendWith(MockitoExtension.class)
class DistributedJobLockServiceImplTest {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    @Mock private DistributedJobLockPersistenceService persistenceService;
    @Mock private DatabaseJobRepository databaseJobRepository;

    @InjectMocks private DistributedJobLockServiceImpl service;

    @Test
    void given_lockAvailable_when_executeWithLock_then_runsWorkAndReleasesLease() {
        when(persistenceService.tryAcquire(anyString(), anyString(), any()))
                .thenReturn(Optional.of("token"));
        when(persistenceService.release(anyString(), anyString())).thenReturn(true);

        var executions = new AtomicInteger();

        var executed =
                service.executeWithLock(
                        "APPLICATION_LISTS_DATABASE_JOB",
                        LEASE_DURATION,
                        (Runnable) executions::incrementAndGet);

        assertTrue(executed);
        assertEquals(1, executions.get());
        verify(persistenceService).release(anyString(), anyString());
    }

    @Test
    void given_lockUnavailable_when_executeWithLock_then_skipsWork() {
        when(persistenceService.tryAcquire(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(databaseJobRepository.existsByName("APPLICATION_LISTS_DATABASE_JOB")).thenReturn(true);

        var executions = new AtomicInteger();

        var executed =
                service.executeWithLock(
                        "APPLICATION_LISTS_DATABASE_JOB",
                        LEASE_DURATION,
                        (Runnable) executions::incrementAndGet);

        assertFalse(executed);
        assertEquals(0, executions.get());
    }

    @Test
    void given_missingJobRow_when_tryAcquire_then_throwsConfigurationError() {
        when(persistenceService.tryAcquire(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(databaseJobRepository.existsByName("MISSING_JOB")).thenReturn(false);

        var exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> service.tryAcquire("MISSING_JOB", LEASE_DURATION));

        assertEquals(CommonAppError.INTERNAL_SERVER_ERROR, exception.getCode());
    }

    @Test
    void given_invalidLeaseDuration_when_tryAcquire_then_throwsValidationError() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.tryAcquire("APPLICATION_LISTS_DATABASE_JOB", Duration.ZERO));
    }

    @Test
    void given_supplierThrows_when_executeWithLock_then_releasesLeaseBeforeRethrow() {
        doReturn(Optional.of("token"))
                .when(persistenceService)
                .tryAcquire(anyString(), anyString(), any());
        when(persistenceService.release(anyString(), anyString())).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.executeWithLock(
                                "APPLICATION_LISTS_DATABASE_JOB",
                                LEASE_DURATION,
                                () -> {
                                    throw new IllegalStateException("boom");
                                }));

        verify(persistenceService).release(anyString(), anyString());
    }
}
