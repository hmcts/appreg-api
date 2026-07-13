package uk.gov.hmcts.appregister.csds.ingress;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLock;
import uk.gov.hmcts.appregister.common.lock.DistributedJobLockService;
import uk.gov.hmcts.appregister.csds.ingress.processor.IDataIngressProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsdsIngressProcessor {
    public static final String DATABASE_JOB_NAME = "CSDS_DATA_INGRESS";

    private final CsdsIngressProperties properties;
    private final CsdsIngressClient ingressClient;
    private final DistributedJobLockService distributedJobLockService;
    private final List<IDataIngressProcessor<?>> processors;

    public boolean runIngress() {
        val lock =
                distributedJobLockService.tryAcquire(
                        DATABASE_JOB_NAME, properties.getLeaseDuration());
        if (lock.isEmpty()) {
            return false;
        }

        try {
            runProcessors(lock.get());
            return true;
        } finally {
            if (!distributedJobLockService.release(lock.get())) {
                log.warn(
                        "Distributed lock release was skipped for job {} because the lease is no longer owned",
                        DATABASE_JOB_NAME);
            }
        }
    }

    private void runProcessors(DistributedJobLock lock) {
        if (processors.isEmpty()) {
            log.info("Skipping CSDS ingress because no data ingress processors are registered");
            return;
        }

        for (var index = 0; index < processors.size(); index++) {
            val processor = processors.get(index);
            if (!processor.enabled()) {
                log.info(
                        "Skipping disabled CSDS ingress processor {} for target {}.{}",
                        processor.datasetName(),
                        processor.targetTable(),
                        processor.targetKeyField());
                continue;
            }
            try {
                runProcessor(processor);
            } catch (RuntimeException ex) {
                log.error(
                        "Skipping CSDS ingress processor {} for target {}.{} after failure",
                        processor.datasetName(),
                        processor.targetTable(),
                        processor.targetKeyField(),
                        ex);
            }
            if (index < processors.size() - 1) {
                renewLock(lock, processor);
            }
        }
    }

    private <T> void runProcessor(IDataIngressProcessor<T> processor) {
        val startedAt = Instant.now();
        log.info(
                "Starting CSDS ingress processor {} for target {}.{}",
                processor.datasetName(),
                processor.targetTable(),
                processor.targetKeyField());

        val rawJson = processor.retrieve(ingressClient);
        val processedData = processor.preProcess(rawJson);
        processor.apply(processedData);

        val duration = Duration.between(startedAt, Instant.now());
        log.info(
                "Completed CSDS ingress processor {} for target {}.{} in {} ms",
                processor.datasetName(),
                processor.targetTable(),
                processor.targetKeyField(),
                duration.toMillis());
    }

    private void renewLock(DistributedJobLock lock, IDataIngressProcessor<?> processor) {
        if (!distributedJobLockService.renew(lock)) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS distributed lease was lost after processor " + processor.datasetName());
        }
    }
}
