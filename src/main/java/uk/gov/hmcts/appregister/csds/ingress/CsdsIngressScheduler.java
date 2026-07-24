package uk.gov.hmcts.appregister.csds.ingress;

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
    private static final String LONDON_TIME_ZONE = "Europe/London";

    private final CsdsIngressProcessor csdsIngressProcessor;

    @Scheduled(
            cron =
                    "0 ${appreg.csds.ingress.schedule.minute:0} ${appreg.csds.ingress.schedule.hour:3} * * *",
            zone = LONDON_TIME_ZONE)
    void runNightlyIngress() {
        log.info("Running scheduled CSDS ingress");

        if (!csdsIngressProcessor.runIngress()) {
            log.info(
                    "Skipping scheduled CSDS ingress because the job is disabled or the distributed lease is not"
                            + " available");
            return;
        }

        log.info("Completed scheduled CSDS ingress");
    }
}
