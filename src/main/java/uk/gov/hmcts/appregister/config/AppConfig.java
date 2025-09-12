package uk.gov.hmcts.appregister.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
