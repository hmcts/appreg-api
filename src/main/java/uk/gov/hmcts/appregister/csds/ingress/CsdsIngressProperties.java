package uk.gov.hmcts.appregister.csds.ingress;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "appreg.csds.ingress")
@Validated
@Getter
@Setter
public class CsdsIngressProperties {
    private String baseUrl;

    private List<String> accessKeys = List.of();

    private String accessKeyHeader = "Api-Key";

    private Duration leaseDuration = Duration.ofMinutes(5L);

    private Duration connectTimeout = Duration.ofSeconds(10L);

    private Duration readTimeout = Duration.ofSeconds(30L);

    private int pageSize = 100;

    private StartupRunner startupRunner = new StartupRunner();

    private Processors processors = new Processors();

    @AssertTrue(
            message =
                    "Configured CSDS ingress requires a baseUrl, accessKeyHeader, at least one accessKey, "
                            + "durations, pageSize and valid processor configuration")
    public boolean isConfigurationValid() {
        if (!isActive()) {
            return true;
        }

        return leaseDuration != null
                && !leaseDuration.isNegative()
                && !leaseDuration.isZero()
                && connectTimeout != null
                && !connectTimeout.isNegative()
                && !connectTimeout.isZero()
                && readTimeout != null
                && !readTimeout.isNegative()
                && !readTimeout.isZero()
                && startupRunner != null
                && processors != null
                && processors.isConfigurationValid()
                && pageSize > 0
                && (!requiresRemoteAccess()
                        || (StringUtils.hasText(baseUrl)
                                && StringUtils.hasText(accessKeyHeader)
                                && accessKeys.stream().anyMatch(StringUtils::hasText)));
    }

    private boolean isActive() {
        return startupRunner != null && startupRunner.isEnabled()
                || processors != null && processors.hasEnabledProcessor();
    }

    private boolean requiresRemoteAccess() {
        return startupRunner != null && startupRunner.isEnabled()
                || processors != null && processors.hasEnabledProcessorWithoutMock();
    }

    @Getter
    @Setter
    public static class StartupRunner {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Processors {
        private ApplicationCodes applicationCodes = new ApplicationCodes();
        private ResolutionCodes resolutionCodes = new ResolutionCodes();

        private boolean isConfigurationValid() {
            return applicationCodes.isConfigurationValid()
                    && resolutionCodes.isConfigurationValid();
        }

        private boolean hasEnabledProcessor() {
            return applicationCodes.isEnabled() || resolutionCodes.isEnabled();
        }

        private boolean hasEnabledProcessorWithoutMock() {
            return applicationCodes != null && applicationCodes.requiresRemoteAccess();
        }
    }

    @Getter
    @Setter
    public static class ProcessorProperties {
        private boolean enabled;

        private String mock;

        private String parameters;

        private String sourceEntityName;

        private String tableName;

        private String primaryKey;

        private String reportingDir;

        protected ProcessorProperties() {
            // Default constructor for configuration binding.
        }

        protected ProcessorProperties(
                String sourceEntityName, String tableName, String primaryKey) {
            this.sourceEntityName = sourceEntityName;
            this.tableName = tableName;
            this.primaryKey = primaryKey;
        }

        protected boolean isConfigurationValid() {
            return !enabled
                    || (StringUtils.hasText(sourceEntityName)
                            && StringUtils.hasText(tableName)
                            && StringUtils.hasText(primaryKey));
        }

        protected boolean requiresRemoteAccess() {
            return enabled && !StringUtils.hasText(mock);
        }
    }

    @Getter
    @Setter
    public static class ApplicationCodes extends ProcessorProperties {
        public ApplicationCodes() {
            super("ApplicationCode", "application_codes", "ac_id");
        }
    }

    @Getter
    @Setter
    public static class ResolutionCodes extends ProcessorProperties {
        public ResolutionCodes() {
            super("ResolutionCode", "resolution_codes_staging", "rc_id");
        }
    }
}
