package uk.gov.hmcts.appregister.csds.ingress;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.schedule",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
class CsdsIngressScheduler {
    private final CsdsIngressProperties properties;
    private final CsdsIngressProcessor csdsIngressProcessor;
    private final CsdsExecutionLogService csdsExecutionLogService;
    private final Clock clock;
    private final ZoneId ukZone;

    @Scheduled(fixedDelayString = "${appreg.csds.ingress.schedule.poll-interval:PT10M}")
    void pollNightlyIngress() {
        if (!isDueNow()) {
            return;
        }
        if (csdsExecutionLogService.hasTerminalStatusToday(
                CsdsIngressProcessor.DATABASE_JOB_NAME)) {
            return;
        }

        var startedAt = LocalDateTime.now(clock.withZone(ukZone));
        var result = csdsIngressProcessor.runScheduledIngress();
        switch (result.status()) {
            case SKIPPED_LOCK_UNAVAILABLE -> {
                log.info(
                        "Skipping scheduled CSDS ingress because the job is disabled or the distributed lease is"
                                + " not available");
            }
            case SUCCEEDED -> logSuccessfulRun(startedAt);
            case FAILED -> {
                log.info("Running scheduled CSDS ingress");
                csdsExecutionLogService.recordFailure(
                        CsdsIngressProcessor.DATABASE_JOB_NAME, startedAt, result.message());
                log.error("Scheduled CSDS ingress failed: {}", result.message());
            }
        }
    }

    private void logSuccessfulRun(LocalDateTime startedAt) {
        log.info("Running scheduled CSDS ingress");
        csdsExecutionLogService.recordSuccess(
                CsdsIngressProcessor.DATABASE_JOB_NAME,
                startedAt,
                "Scheduled CSDS ingress completed successfully");
        log.info("Completed scheduled CSDS ingress");
    }

    private boolean isDueNow() {
        var nowUk = LocalDateTime.now(clock.withZone(ukZone));
        var schedule = properties.getSchedule();
        var scheduledTime = LocalTime.of(schedule.getHour(), schedule.getMinute());
        return !nowUk.toLocalTime().isBefore(scheduledTime);
    }
}
