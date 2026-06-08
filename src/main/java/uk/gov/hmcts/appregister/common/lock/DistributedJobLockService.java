package uk.gov.hmcts.appregister.common.lock;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface DistributedJobLockService {
    Optional<DistributedJobLock> tryAcquire(String jobName, Duration leaseDuration);

    boolean renew(DistributedJobLock lock);

    boolean release(DistributedJobLock lock);

    boolean executeWithLock(String jobName, Duration leaseDuration, Runnable runnable);

    <T> Optional<T> executeWithLock(String jobName, Duration leaseDuration, Supplier<T> supplier);
}
