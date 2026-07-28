package uk.gov.hmcts.appregister.csds.ingress;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final CsdsExecutionLogService csdsExecutionLogService;
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

    public boolean hasTerminalStatusToday() {
        return csdsExecutionLogService.hasTerminalStatusToday(DATABASE_JOB_NAME);
    }

    public ScheduledRunResult runScheduledIngress(LocalDateTime startedAt) {
        val lock =
                distributedJobLockService.tryAcquire(
                        DATABASE_JOB_NAME, properties.getLeaseDuration());
        if (lock.isEmpty()) {
            return ScheduledRunResult.skippedLockUnavailable();
        }

        try {
            log.info("Running scheduled CSDS ingress");
            var failedProcessors = runProcessors(lock.get());
            if (!failedProcessors.isEmpty()) {
                return recordFailure(
                        startedAt, "Failed processors: " + String.join(", ", failedProcessors));
            }
            return recordSuccess(startedAt);
        } catch (RuntimeException ex) {
            return recordFailure(startedAt, summarizeFailure(ex));
        } finally {
            if (!distributedJobLockService.release(lock.get())) {
                log.warn(
                        "Distributed lock release was skipped for job {} because the lease is no longer owned",
                        DATABASE_JOB_NAME);
            }
        }
    }

    private List<String> runProcessors(DistributedJobLock lock) {
        if (processors.isEmpty()) {
            log.info("Skipping CSDS ingress because no data ingress processors are registered");
            return List.of();
        }

        var failedProcessors = new ArrayList<String>();
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
                        "Skipping CSDS ingress processor {} for target {}.{} after failure: {}",
                        processor.datasetName(),
                        processor.targetTable(),
                        processor.targetKeyField(),
                        summarizeFailure(ex));
                log.debug(
                        "CSDS ingress processor {} failed for target {}.{}",
                        processor.datasetName(),
                        processor.targetTable(),
                        processor.targetKeyField(),
                        ex);
                failedProcessors.add(processor.datasetName());
            }
            if (index < processors.size() - 1) {
                renewLock(lock, processor);
            }
        }
        return List.copyOf(failedProcessors);
    }

    private String summarizeFailure(RuntimeException ex) {
        if (ex.getCause() == null
                || ex.getCause().getMessage() == null
                || ex.getCause().getMessage().isBlank()) {
            return ex.getMessage();
        }
        return "%s (cause: %s)".formatted(ex.getMessage(), ex.getCause().getMessage());
    }

    private ScheduledRunResult recordSuccess(LocalDateTime startedAt) {
        csdsExecutionLogService.recordSuccess(
                DATABASE_JOB_NAME, startedAt, "Scheduled CSDS ingress completed successfully");
        return ScheduledRunResult.succeeded();
    }

    private ScheduledRunResult recordFailure(LocalDateTime startedAt, String message) {
        csdsExecutionLogService.recordFailure(DATABASE_JOB_NAME, startedAt, message);
        return ScheduledRunResult.failed(message);
    }

    private <T> void runProcessor(IDataIngressProcessor<T> processor) {
        log.info(
                "Starting CSDS ingress processor {} for target {}.{}",
                processor.datasetName(),
                processor.targetTable(),
                processor.targetKeyField());

        val rawJson = processor.retrieve(ingressClient);
        try {
            processor.backup();
        } catch (RuntimeException ex) {
            log.error(
                    "Failed CSDS backup step for processor {}. Continuing ingress.",
                    processor.datasetName(),
                    ex);
        }
        val processedData = processor.preProcess(rawJson);
        processor.apply(processedData);

        val startedAt = Instant.now();
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

    public record ScheduledRunResult(ScheduledRunStatus status, String message) {
        static ScheduledRunResult skippedLockUnavailable() {
            return new ScheduledRunResult(ScheduledRunStatus.SKIPPED_LOCK_UNAVAILABLE, null);
        }

        static ScheduledRunResult succeeded() {
            return new ScheduledRunResult(ScheduledRunStatus.SUCCEEDED, null);
        }

        static ScheduledRunResult failed(String message) {
            return new ScheduledRunResult(ScheduledRunStatus.FAILED, message);
        }
    }

    public enum ScheduledRunStatus {
        SKIPPED_LOCK_UNAVAILABLE,
        SUCCEEDED,
        FAILED
    }
}
