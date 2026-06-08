package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.Duration;
import java.util.Optional;

public interface DatabaseJobLockRepositoryCustom {
    Optional<String> tryAcquireLease(String jobName, String token, Duration leaseDuration);

    boolean renewLease(String jobName, String token, Duration leaseDuration);

    boolean releaseLease(String jobName, String token);
}
