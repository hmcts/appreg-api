package uk.gov.hmcts.appregister.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

    static final String LOCAL_DEBUG_MARKER = "appreg-local-debug-only.properties";

    private static final String LOCAL_DEBUG_ONLY_MESSAGE =
            "The 'nosecurity' profile can only be used with the local debug Gradle runtime";

    private static final String LOOPBACK_ONLY_MESSAGE =
            "The 'nosecurity' profile requires server.address to be a valid loopback address";

    public NoSecurityConfig(@Value("${server.address:127.0.0.1}") String serverAddress) {
        verifyLocalDebugRuntime(Thread.currentThread().getContextClassLoader());
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
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    static void verifyLocalDebugRuntime(ClassLoader classLoader) {
        ClassLoader resourceLoader =
                classLoader == null ? NoSecurityConfig.class.getClassLoader() : classLoader;
        if (resourceLoader.getResource(LOCAL_DEBUG_MARKER) == null) {
            throw new NoSecurityConfigurationException(LOCAL_DEBUG_ONLY_MESSAGE);
        }
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
