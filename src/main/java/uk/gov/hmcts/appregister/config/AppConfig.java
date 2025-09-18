package uk.gov.hmcts.appregister.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

@Configuration
public class AppConfig {
    /**
     * Allows the clock to be modified so that we can test time sensitive code.
     *
     * @return The clock that is returned which is a UTC clock
     */
    @Bean
    public Clock getClock() {
        return Clock.systemUTC();
    }

    /**
     * Defines a audit lifecycle listener that can be called when working with the {@link
     * uk.gov.hmcts.appregister.audit.service.AuditOperationService}.
     */
    @Bean
    public AuditOperationLifecycleListener getSystemLevelAuditListener() {
        return new AuditOperationSlf4jLogger();
    }
}
