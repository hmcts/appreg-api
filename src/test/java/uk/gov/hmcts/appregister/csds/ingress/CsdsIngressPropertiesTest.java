package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsdsIngressPropertiesTest {

    @Test
    void given_manualIngestWithMockOnly_when_validate_then_remoteCredentialsAreNotRequired() {
        var properties = baseProperties();
        properties.getProcessors().getApplicationCodes().setEnabled(true);
        properties.getProcessors().getApplicationCodes().setMock("csds/application_codes.json");

        assertThat(properties.isConfigurationValid()).isTrue();
    }

    @Test
    void given_enabledProcessorWithoutMock_when_validate_then_remoteCredentialsAreRequired() {
        var properties = baseProperties();
        properties.getProcessors().getApplicationCodes().setEnabled(true);
        properties.getProcessors().getApplicationCodes().setMock(null);

        assertThat(properties.isConfigurationValid()).isFalse();
    }

    @Test
    void given_enabledProcessorWithoutMockAndCredentials_when_validate_then_configurationIsValid() {
        var properties = baseProperties();
        properties.getProcessors().getApplicationCodes().setEnabled(true);
        properties.getProcessors().getApplicationCodes().setMock(null);
        properties.setBaseUrl("https://example.test/api/rest");
        properties.setAccessKeyHeader("Api-Key");
        properties.setAccessKeys(List.of("secret"));

        assertThat(properties.isConfigurationValid()).isTrue();
    }

    @Test
    void given_startupRunnerEnabled_when_validate_then_remoteCredentialsAreRequired() {
        var properties = baseProperties();
        properties.getStartupRunner().setEnabled(true);

        assertThat(properties.isConfigurationValid()).isFalse();
    }

    private CsdsIngressProperties baseProperties() {
        var properties = new CsdsIngressProperties();
        properties.setLeaseDuration(Duration.ofMinutes(5));
        properties.setConnectTimeout(Duration.ofSeconds(10));
        properties.setReadTimeout(Duration.ofSeconds(30));
        properties.setPageSize(100);
        return properties;
    }
}
