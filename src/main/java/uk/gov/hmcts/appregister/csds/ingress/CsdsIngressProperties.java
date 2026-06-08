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

    private String accessKeyHeader = "x-api-key";

    private Duration leaseDuration = Duration.ofMinutes(5);

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration readTimeout = Duration.ofSeconds(30);

    @AssertTrue(message = "Enabled CSDS ingress requires a baseUrl and exactly two accessKeys")
    public boolean isConfigurationValid() {
        if (!enabled) {
            return true;
        }

        return StringUtils.hasText(baseUrl)
                && StringUtils.hasText(accessKeyHeader)
                && accessKeys.stream().filter(StringUtils::hasText).count() == 2
                && leaseDuration != null
                && !leaseDuration.isNegative()
                && !leaseDuration.isZero()
                && connectTimeout != null
                && !connectTimeout.isNegative()
                && !connectTimeout.isZero()
                && readTimeout != null
                && !readTimeout.isNegative()
                && !readTimeout.isZero();
    }
}
