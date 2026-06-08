package uk.gov.hmcts.appregister.common.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

class DistributedJobLockServiceIntegrationTest extends BaseRepositoryTest {
    private static final String JOB_NAME = "APPLICATION_LISTS_DATABASE_JOB";
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    @Autowired private DistributedJobLockService distributedJobLockService;
    @Autowired private DatabaseJobRepository databaseJobRepository;

    @BeforeEach
    void resetDatabaseJobLockRow() {
        restoreDatabaseJobLockRow();
    }

    @AfterEach
    void cleanDatabaseJobLockRow() {
        restoreDatabaseJobLockRow();
    }

    private void restoreDatabaseJobLockRow() {
        var job = databaseJobRepository.findByName(JOB_NAME);
        job.setEnabled(YesOrNo.YES);
        job.setMetadata(null);
        job.setLastRan(null);
        databaseJobRepository.save(job);
    }

    @Test
    void given_enabledUnlockedJob_when_tryAcquire_then_returnsLockAndUpdatesRow() {
        var lock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION);

        assertTrue(lock.isPresent());
        assertEquals(JOB_NAME, lock.get().jobName());
        assertNotNull(lock.get().token());

        var job = databaseJobRepository.findByName(JOB_NAME);
        assertEquals(lock.get().token(), job.getMetadata());
        assertNotNull(job.getLastRan());
    }

    @Test
    void given_activeLease_when_tryAcquire_then_returnsEmpty() {
        var firstLock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION);

        var secondLock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION);

        assertTrue(firstLock.isPresent());
        assertTrue(secondLock.isEmpty());
    }

    @Test
    void given_staleLease_when_tryAcquire_then_reacquiresLock() {
        DatabaseJob job = databaseJobRepository.findByName(JOB_NAME);
        job.setEnabled(YesOrNo.YES);
        job.setMetadata("stale-token");
        job.setLastRan(OffsetDateTime.now().minusMinutes(10));
        databaseJobRepository.save(job);

        var reacquiredLock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION);

        assertTrue(reacquiredLock.isPresent());
        assertTrue(reacquiredLock.get().token().length() <= 64);
        assertFalse("stale-token".equals(reacquiredLock.get().token()));
    }

    @Test
    void given_currentOwner_when_renewAndRelease_then_onlyMatchingTokenSucceeds() {
        var lock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION).orElseThrow();

        assertTrue(distributedJobLockService.renew(lock));
        assertFalse(
                distributedJobLockService.renew(
                        new DistributedJobLock(
                                lock.jobName(), "wrong-token", lock.leaseDuration())));
        assertFalse(
                distributedJobLockService.release(
                        new DistributedJobLock(
                                lock.jobName(), "wrong-token", lock.leaseDuration())));
        assertTrue(distributedJobLockService.release(lock));

        var job = databaseJobRepository.findByName(JOB_NAME);
        assertNull(job.getMetadata());
        assertNull(job.getLastRan());
    }

    @Test
    void given_disabledJob_when_tryAcquire_then_returnsEmpty() {
        DatabaseJob job = databaseJobRepository.findByName(JOB_NAME);
        job.setEnabled(YesOrNo.NO);
        databaseJobRepository.save(job);

        var lock = distributedJobLockService.tryAcquire(JOB_NAME, LEASE_DURATION);

        assertTrue(lock.isEmpty());
    }

    @Test
    void given_missingJobRow_when_tryAcquire_then_throwsConfigurationError() {
        assertThrows(
                AppRegistryException.class,
                () -> distributedJobLockService.tryAcquire("MISSING_JOB", LEASE_DURATION));
    }
}
