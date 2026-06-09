package uk.gov.hmcts.appregister.common.lock;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedJobLockServiceImpl implements DistributedJobLockService {
    private final DistributedJobLockPersistenceService persistenceService;
    private final DatabaseJobRepository databaseJobRepository;

    @Override
    public Optional<DistributedJobLock> tryAcquire(String jobName, Duration leaseDuration) {
        validateLeaseDuration(leaseDuration);

        var token = UUID.randomUUID().toString();
        var acquired = persistenceService.tryAcquire(jobName, token, leaseDuration);

        if (acquired.isPresent()) {
            return Optional.of(new DistributedJobLock(jobName, token, leaseDuration));
        }

        if (!databaseJobRepository.existsByName(jobName)) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "No database_jobs row was found for scheduled job " + jobName);
        }

        return Optional.empty();
    }

    @Override
    public boolean renew(DistributedJobLock lock) {
        validateLock(lock);
        return persistenceService.renew(lock.jobName(), lock.token(), lock.leaseDuration());
    }

    @Override
    public boolean release(DistributedJobLock lock) {
        validateLock(lock);
        return persistenceService.release(lock.jobName(), lock.token());
    }

    @Override
    public boolean executeWithLock(String jobName, Duration leaseDuration, Runnable runnable) {
        return executeWithLock(
                        jobName,
                        leaseDuration,
                        () -> {
                            runnable.run();
                            return true;
                        })
                .orElse(false);
    }

    @Override
    public <T> Optional<T> executeWithLock(
            String jobName, Duration leaseDuration, Supplier<T> supplier) {
        var lock = tryAcquire(jobName, leaseDuration);
        if (lock.isEmpty()) {
            log.info(
                    "Skipping scheduled job {} because its distributed lease is not available",
                    jobName);
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(supplier.get());
        } finally {
            if (!release(lock.get())) {
                log.warn(
                        "Distributed lock release was skipped for job {} because the lease is no longer owned",
                        jobName);
            }
        }
    }

    private static void validateLock(DistributedJobLock lock) {
        if (lock == null) {
            throw new IllegalArgumentException("Distributed job lock must not be null");
        }

        validateLeaseDuration(lock.leaseDuration());
    }

    private static void validateLeaseDuration(Duration leaseDuration) {
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("Lease duration must be greater than zero");
        }
    }
}
