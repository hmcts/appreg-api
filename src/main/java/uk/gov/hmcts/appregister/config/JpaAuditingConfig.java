package uk.gov.hmcts.appregister.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import uk.gov.hmcts.appregister.common.security.UserProvider;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<String> auditorAware(UserProvider user) {
        return () -> Optional.of(user.getUserId());
    }
}
