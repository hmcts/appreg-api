package uk.gov.hmcts.appregister.common.lock;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;

@Component
@RequiredArgsConstructor
class DistributedJobLockPersistenceService {
    private final DatabaseJobRepository databaseJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> tryAcquire(String jobName, String token, Duration leaseDuration) {
        return databaseJobRepository.tryAcquireLease(jobName, token, leaseDuration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(String jobName, String token, Duration leaseDuration) {
        return databaseJobRepository.renewLease(jobName, token, leaseDuration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean release(String jobName, String token) {
        return databaseJobRepository.releaseLease(jobName, token);
    }
}
