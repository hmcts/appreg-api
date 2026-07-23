package uk.gov.hmcts.appregister.config;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs a narrow startup diagnostic for secret-backed configuration values that are useful.
 */
@Component
@Slf4j
public class StartupSecretDiagnosticsLogger implements ApplicationRunner {
    static final String CSDS_BASE_URL = "CSDS_BASE_URL";
    private static final String CSDS_BASE_URL_DEFAULT = "http://noop";

    private final Environment environment;
    private final Path mountedSecretsPath;

    public StartupSecretDiagnosticsLogger(
            Environment environment,
            @Value("${appreg.startup-secret-diagnostics.mount-path:/mnt/secrets/appreg}")
                    String mountedSecretsPath) {
        this.environment = environment;
        this.mountedSecretsPath = Path.of(mountedSecretsPath);
    }

    @Override
    public void run(ApplicationArguments args) {
        logDiagnostic(inspect(CSDS_BASE_URL, CSDS_BASE_URL_DEFAULT));
    }

    private StartupSecretDiagnostic inspect(String propertyName, String defaultValue) {
        var resolvedValue = environment.getProperty(propertyName, defaultValue);
        var mountedSecretPath = mountedSecretsPath.resolve(propertyName);

        return new StartupSecretDiagnostic(
                propertyName,
                resolvedValue,
                defaultValue.equals(resolvedValue),
                Files.exists(mountedSecretPath),
                Files.isReadable(mountedSecretPath));
    }

    private void logDiagnostic(StartupSecretDiagnostic diagnostic) {
        log.info(
                "Startup secret diagnostic: property={} resolvedValue={} usingDefault={} mountedFilePresent={}"
                        + " mountedFileReadable={}",
                diagnostic.propertyName(),
                diagnostic.resolvedValue(),
                diagnostic.usingDefault(),
                diagnostic.mountedFilePresent(),
                diagnostic.mountedFileReadable());
    }

    record StartupSecretDiagnostic(
            String propertyName,
            String resolvedValue,
            boolean usingDefault,
            boolean mountedFilePresent,
            boolean mountedFileReadable) {}
}
