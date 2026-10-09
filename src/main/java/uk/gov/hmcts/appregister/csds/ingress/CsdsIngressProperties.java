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

    // Retained for configuration compatibility; retrieval now uses the reported dataset count.
    private int pageSize = 100;

    private StartupRunner startupRunner = new StartupRunner();

    private Schedule schedule = new Schedule();

    private Processors processors = new Processors();

    public String getNightlyCron() {
        return "0 %d %d * * *".formatted(schedule.getMinute(), schedule.getHour());
    }

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
    public static class Schedule {
        private int hour = 3;
        private int minute = 0;
        private Duration pollInterval = Duration.ofMinutes(10L);
    }

    @Getter
    @Setter
    public static class Processors {
        private boolean reportRaw;

        private ApplicationCodes applicationCodes = new ApplicationCodes();
        private ResolutionCodes resolutionCodes = new ResolutionCodes();
        private Fee fee = new Fee();
        private NationalCourtHouses nationalCourtHouses = new NationalCourtHouses();
        private StandardApplicants standardApplicants = new StandardApplicants();

        private boolean isConfigurationValid() {
            return applicationCodes.isConfigurationValid()
                    && resolutionCodes.isConfigurationValid()
                    && fee.isConfigurationValid()
                    && nationalCourtHouses.isConfigurationValid()
                    && standardApplicants.isConfigurationValid();
        }

        private boolean hasEnabledProcessor() {
            return applicationCodes.isEnabled()
                    || resolutionCodes.isEnabled()
                    || fee.isEnabled()
                    || nationalCourtHouses.isEnabled()
                    || standardApplicants.isEnabled();
        }

        private boolean hasEnabledProcessorWithoutMock() {
            return applicationCodes.requiresRemoteAccess()
                    || resolutionCodes.requiresRemoteAccess()
                    || fee.requiresRemoteAccess()
                    || nationalCourtHouses.requiresRemoteAccess()
                    || standardApplicants.requiresRemoteAccess();
        }
    }

    @Getter
    @Setter
    public static class ProcessorProperties {
        private boolean enabled;

        private String mock;

        private String parameters;

        private String sourceEntityName;

        private String ingressTarget;

        private String backupSource;

        private String backupTarget;

        private List<String> primaryKeys;

        private String reportingDir;

        protected ProcessorProperties() {
            // Default constructor for configuration binding.
        }

        protected ProcessorProperties(
                String sourceEntityName, String ingressTarget, List<String> primaryKeys) {
            this.sourceEntityName = sourceEntityName;
            this.ingressTarget = ingressTarget;
            this.primaryKeys = primaryKeys;
        }

        protected boolean isConfigurationValid() {
            return !enabled
                    || (StringUtils.hasText(sourceEntityName)
                            && StringUtils.hasText(ingressTarget)
                            && primaryKeys != null
                            && !primaryKeys.isEmpty()
                            && primaryKeys.stream().allMatch(StringUtils::hasText)
                            && primaryKeys.stream().distinct().count() == primaryKeys.size());
        }

        protected boolean requiresRemoteAccess() {
            return enabled && !StringUtils.hasText(mock);
        }
    }

    @Getter
    @Setter
    public static class ApplicationCodes extends ProcessorProperties {
        public ApplicationCodes() {
            super("ApplicationCode", "application_codes_staging", List.of("application_code"));
        }
    }

    @Getter
    @Setter
    public static class ResolutionCodes extends ProcessorProperties {
        public ResolutionCodes() {
            super("ResolutionCode", "resolution_codes_staging", List.of("resolution_code"));
        }
    }

    @Getter
    @Setter
    public static class Fee extends ProcessorProperties {
        public Fee() {
            super("CivilFee", "fee_staging", List.of("fee_id"));
        }
    }

    @Getter
    @Setter
    public static class NationalCourtHouses extends ProcessorProperties {
        public NationalCourtHouses() {
            super("Court", "national_court_houses_staging", List.of("nch_id"));
        }
    }

    @Getter
    @Setter
    public static class StandardApplicants extends ProcessorProperties {
        public StandardApplicants() {
            super("DA_GetStandardApplicant", "standard_applicants_staging", List.of("sa_id"));
        }
    }
}
