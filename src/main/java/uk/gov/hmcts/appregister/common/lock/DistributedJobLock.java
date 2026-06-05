package uk.gov.hmcts.appregister.common.lock;

import java.time.Duration;

public record DistributedJobLock(String jobName, String token, Duration leaseDuration) {}
