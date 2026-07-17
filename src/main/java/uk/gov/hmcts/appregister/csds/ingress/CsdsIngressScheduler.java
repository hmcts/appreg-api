package uk.gov.hmcts.appregister.csds.ingress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class CsdsIngressScheduler {
    private static final String NIGHTLY_INGRESS_CRON = "0 0 20 * * *";
    private static final String LONDON_TIME_ZONE = "Europe/London";

    private final CsdsIngressProcessor csdsIngressProcessor;

    @Scheduled(cron = NIGHTLY_INGRESS_CRON, zone = LONDON_TIME_ZONE)
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
