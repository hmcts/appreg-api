package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NoSecurityConfigTest {

    @Test
    void verifyLoopbackAddress_allowsIpv4Loopback() {
        assertThatCode(() -> NoSecurityConfig.verifyLoopbackAddress("127.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyLoopbackAddress_allowsIpv6Loopback() {
        assertThatCode(() -> NoSecurityConfig.verifyLoopbackAddress("::1"))
                .doesNotThrowAnyException();
    }

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
}
