package uk.gov.hmcts.appregister.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * Local-only configuration that disables authentication when the {@code nosecurity} profile is
 * active.
 */
@Configuration
@Profile("nosecurity")
@Slf4j
public class NoSecurityConfig {

    private static final String LOOPBACK_ONLY_MESSAGE =
            "The 'nosecurity' profile requires server.address to be a valid loopback address";

    public NoSecurityConfig(@Value("${server.address:127.0.0.1}") String serverAddress) {
        InetAddress bindAddress = verifyLoopbackAddress(serverAddress);
        log.warn(
                "The 'nosecurity' profile is active. Authentication is disabled and the API "
                        + "is bound to {} only.",
                bindAddress.getHostAddress());
    }

    @Bean
    @SuppressWarnings("java:S4502")
    SecurityFilterChain noSecurityFilterChain(HttpSecurity http) {
        // This profile is local-only, disables authentication entirely, and is restricted to
        // loopback binding by verifyLoopbackAddress.
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    static InetAddress verifyLoopbackAddress(String serverAddress) {
        if (!StringUtils.hasText(serverAddress)) {
            throw new NoSecurityConfigurationException(LOOPBACK_ONLY_MESSAGE);
        }

        try {
            InetAddress bindAddress = InetAddress.getByName(serverAddress);
            if (!bindAddress.isLoopbackAddress()) {
                throw new NoSecurityConfigurationException(LOOPBACK_ONLY_MESSAGE);
            }
            return bindAddress;
        } catch (UnknownHostException exception) {
            throw new NoSecurityConfigurationException(LOOPBACK_ONLY_MESSAGE, exception);
        }
    }
}
