package uk.gov.hmcts.appregister.csds.ingress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("nosecurity")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.startup-runner",
        name = "enabled",
        havingValue = "true")
class CsdsIngressStartupRunner implements ApplicationRunner {
    private final CsdsIngressProcessor csdsIngressProcessor;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running CSDS ingress on application startup");

        try {
            var executed = csdsIngressProcessor.runIngress();
            if (!executed) {
                log.info(
                        "Skipped CSDS ingress startup run because the distributed lock was not acquired");
            }
        } finally {
            log.info("Shutting down application after CSDS ingress startup run");
            applicationContext.close();
        }
    }
}
