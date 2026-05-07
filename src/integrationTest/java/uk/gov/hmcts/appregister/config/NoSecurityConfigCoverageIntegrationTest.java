package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;

class NoSecurityConfigCoverageIntegrationTest {

    @Test
    void verifyLocalDebugRuntime_allowsDefaultClasspathWithMarker() {
        assertThatCode(() -> NoSecurityConfig.verifyLocalDebugRuntime(null))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyLocalDebugRuntime_rejectsClasspathWithoutMarker() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLocalDebugRuntime(emptyClassLoader()))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("local debug");
    }

    @Test
    void verifyLocalDebugRuntime_allowsClasspathWithMarker() {
        assertThatCode(() -> NoSecurityConfig.verifyLocalDebugRuntime(markerClassLoader()))
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

    @Test
    void verifyLoopbackAddress_rejectsUnknownHost() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLoopbackAddress("not-a-real-host.invalid"))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("loopback")
                .hasCauseInstanceOf(UnknownHostException.class);
    }

    private static ClassLoader markerClassLoader() {
        return new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                if (NoSecurityConfig.LOCAL_DEBUG_MARKER.equals(name)) {
                    return NoSecurityConfigCoverageIntegrationTest.class.getResource(
                            "NoSecurityConfigCoverageIntegrationTest.class");
                }
                return null;
            }
        };
    }

    private static ClassLoader emptyClassLoader() {
        return new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return null;
            }
        };
    }
}
