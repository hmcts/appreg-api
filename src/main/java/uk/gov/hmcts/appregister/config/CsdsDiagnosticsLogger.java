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
 * Logs startup diagnostics for the CSDS ingress configuration and its secret-backed inputs.
 */
@Slf4j
@Component
public class CsdsDiagnosticsLogger implements ApplicationRunner {
    static final String CSDS_BASE_URL = "CSDS_BASE_URL";
    static final String CSDS_KEY_1 = "CSDS_KEY_1";
    static final String CSDS_KEY_2 = "CSDS_KEY_2";
    static final String CSDS_SCHEDULE_HOUR = "CSDS_SCHEDULE_HOUR";
    static final String CSDS_SCHEDULE_MINUTE = "CSDS_SCHEDULE_MINUTE";
    private static final String CSDS_BASE_URL_DEFAULT = "http://noop";
    private static final String CSDS_KEY_1_DEFAULT = "missing_1";
    private static final String CSDS_KEY_2_DEFAULT = "missing_2";
    private static final String CSDS_SCHEDULE_HOUR_DEFAULT = "3";
    private static final String CSDS_SCHEDULE_MINUTE_DEFAULT = "0";
    private static final String CSDS_SCHEDULE_ENABLED_PROPERTY =
            "appreg.csds.ingress.schedule.enabled";
    private static final String CSDS_SCHEDULE_POLL_INTERVAL_PROPERTY =
            "appreg.csds.ingress.schedule.poll-interval";
    private static final String CSDS_LEASE_DURATION_PROPERTY = "appreg.csds.ingress.lease-duration";
    private static final String CSDS_PAGE_SIZE_PROPERTY = "appreg.csds.ingress.page-size";
    private static final String CSDS_APPLICATION_CODES_ENABLED_PROPERTY =
            "appreg.csds.ingress.processors.application-codes.enabled";
    private static final String CSDS_RESOLUTION_CODES_ENABLED_PROPERTY =
            "appreg.csds.ingress.processors.resolution-codes.enabled";
    private static final String CSDS_FEE_ENABLED_PROPERTY =
            "appreg.csds.ingress.processors.fee.enabled";
    private static final String CSDS_STANDARD_APPLICANTS_ENABLED_PROPERTY =
            "appreg.csds.ingress.processors.standard-applicants.enabled";
    private static final String CSDS_NATIONAL_COURT_HOUSES_ENABLED_PROPERTY =
            "appreg.csds.ingress.processors.national-court-houses.enabled";

    private final Environment environment;
    private final Path mountedSecretsPath;

    public CsdsDiagnosticsLogger(
            Environment environment,
            @Value("${appreg.startup-secret-diagnostics.mount-path:/mnt/secrets/appreg}")
                    String mountedSecretsPath) {
        this.environment = environment;
        this.mountedSecretsPath = Path.of(mountedSecretsPath);
    }

    @Override
    public void run(ApplicationArguments args) {
        logSecretDiagnostic(inspectSecret(CSDS_BASE_URL, CSDS_BASE_URL_DEFAULT));
        logSecretDiagnostic(inspectSecret(CSDS_KEY_1, CSDS_KEY_1_DEFAULT));
        logSecretDiagnostic(inspectSecret(CSDS_KEY_2, CSDS_KEY_2_DEFAULT));
        logSecretDiagnostic(inspectSecret(CSDS_SCHEDULE_HOUR, CSDS_SCHEDULE_HOUR_DEFAULT));
        logSecretDiagnostic(inspectSecret(CSDS_SCHEDULE_MINUTE, CSDS_SCHEDULE_MINUTE_DEFAULT));
        logConfigDiagnostic();
    }

    private SecretDiagnostic inspectSecret(String propertyName, String defaultValue) {
        var resolvedValue = environment.getProperty(propertyName, defaultValue);
        var mountedSecretPath = mountedSecretsPath.resolve(propertyName);

        return new SecretDiagnostic(
                propertyName,
                resolvedValue,
                defaultValue.equals(resolvedValue),
                Files.exists(mountedSecretPath),
                Files.isReadable(mountedSecretPath));
    }

    private void logSecretDiagnostic(SecretDiagnostic diagnostic) {
        log.info(
                "CSDS secret diagnostic: property={} resolvedValue={} usingDefault={} mountedFilePresent={}"
                        + " mountedFileReadable={}",
                diagnostic.propertyName(),
                maskIfSensitive(diagnostic.propertyName(), diagnostic.resolvedValue()),
                diagnostic.usingDefault(),
                diagnostic.mountedFilePresent(),
                diagnostic.mountedFileReadable());
    }

    private String maskIfSensitive(String propertyName, String resolvedValue) {
        if (!CSDS_KEY_1.equals(propertyName) && !CSDS_KEY_2.equals(propertyName)) {
            return resolvedValue;
        }
        if (resolvedValue == null || resolvedValue.length() <= 4) {
            return resolvedValue;
        }
        return resolvedValue.substring(0, 2)
                + "***"
                + resolvedValue.substring(resolvedValue.length() - 2);
    }

    private void logConfigDiagnostic() {
        log.info(
                "CSDS config diagnostic: scheduleEnabled={} scheduleHour={} scheduleMinute={}"
                        + " schedulePollInterval={} leaseDuration={} pageSize={}"
                        + " processors=application-codes:{} resolution-codes:{} fee:{}"
                        + " standard-applicants:{} national-court-houses:{}",
                environment.getProperty(CSDS_SCHEDULE_ENABLED_PROPERTY, Boolean.class, true),
                environment.getProperty(CSDS_SCHEDULE_HOUR, CSDS_SCHEDULE_HOUR_DEFAULT),
                environment.getProperty(CSDS_SCHEDULE_MINUTE, CSDS_SCHEDULE_MINUTE_DEFAULT),
                environment.getProperty(CSDS_SCHEDULE_POLL_INTERVAL_PROPERTY, "PT10M"),
                environment.getProperty(CSDS_LEASE_DURATION_PROPERTY, "PT5M"),
                environment.getProperty(CSDS_PAGE_SIZE_PROPERTY, Integer.class, 100),
                environment.getProperty(
                        CSDS_APPLICATION_CODES_ENABLED_PROPERTY, Boolean.class, true),
                environment.getProperty(
                        CSDS_RESOLUTION_CODES_ENABLED_PROPERTY, Boolean.class, true),
                environment.getProperty(CSDS_FEE_ENABLED_PROPERTY, Boolean.class, true),
                environment.getProperty(
                        CSDS_STANDARD_APPLICANTS_ENABLED_PROPERTY, Boolean.class, true),
                environment.getProperty(
                        CSDS_NATIONAL_COURT_HOUSES_ENABLED_PROPERTY, Boolean.class, true));
    }

    record SecretDiagnostic(
            String propertyName,
            String resolvedValue,
            boolean usingDefault,
            boolean mountedFilePresent,
            boolean mountedFileReadable) {}
}
