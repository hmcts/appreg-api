package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;

class NoSecurityConfigCoverageIntegrationTest {

    @Test
    void verifyLoopbackAddress_rejectsBlankAddress() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLoopbackAddress(" "))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void verifyLoopbackAddress_rejectsNonLoopbackAddress() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLoopbackAddress("0.0.0.0"))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void verifyLoopbackAddress_rejectsUnknownHost() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLoopbackAddress("not-a-real-host.invalid"))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("loopback")
                .hasCauseInstanceOf(UnknownHostException.class);
    }
}
