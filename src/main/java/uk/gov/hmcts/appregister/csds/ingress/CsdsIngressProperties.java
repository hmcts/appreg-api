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
    private boolean enabled;

    private String baseUrl;

    private List<String> accessKeys = List.of();

    private String accessKeyHeader = "Api-Key";

    private Duration leaseDuration = Duration.ofMinutes(5L);

    private Duration connectTimeout = Duration.ofSeconds(10L);

    private Duration readTimeout = Duration.ofSeconds(30L);

    private int pageSize = 100;

    private StartupRunner startupRunner = new StartupRunner();

    private Processors processors = new Processors();

    @AssertTrue(message = "Enabled CSDS ingress requires a baseUrl and at least one accessKey")
    public boolean isConfigurationValid() {
        if (!enabled) {
            return true;
        }

        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(accessKeyHeader)
                && accessKeys.stream().anyMatch(StringUtils::hasText)
                && leaseDuration != null
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
                && pageSize > 0;
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

        private boolean isConfigurationValid() {
            return applicationCodes != null && applicationCodes.isConfigurationValid();
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
    }

    @Getter
    @Setter
    public static class ApplicationCodes extends ProcessorProperties {
        public ApplicationCodes() {
            super("ApplicationCode", "application_codes", "ac_id");
        }
    }
}
