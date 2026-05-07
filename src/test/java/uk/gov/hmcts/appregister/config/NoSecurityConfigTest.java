package uk.gov.hmcts.appregister.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class NoSecurityConfigTest {

    @Test
    void constructor_allowsLocalDebugRuntimeAndLoopbackAddress() {
        assertThatCode(
                        () ->
                                withContextClassLoader(
                                        markerClassLoader(),
                                        () -> new NoSecurityConfig("127.0.0.1")))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_rejectsRuntimeWithoutLocalDebugMarker() {
        assertThatThrownBy(
                        () ->
                                withContextClassLoader(
                                        emptyClassLoader(),
                                        () -> new NoSecurityConfig("127.0.0.1")))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("local debug");
    }

    @Test
    void constructor_rejectsNonLoopbackAddress() {
        assertThatThrownBy(
                        () ->
                                withContextClassLoader(
                                        markerClassLoader(), () -> new NoSecurityConfig("0.0.0.0")))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("loopback");
    }

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

    @Test
    void verifyLocalDebugRuntime_allowsClasspathWithMarker() {
        assertThatCode(() -> NoSecurityConfig.verifyLocalDebugRuntime(markerClassLoader()))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyLocalDebugRuntime_rejectsClasspathWithoutMarker() {
        assertThatThrownBy(() -> NoSecurityConfig.verifyLocalDebugRuntime(emptyClassLoader()))
                .isInstanceOf(NoSecurityConfigurationException.class)
                .hasMessageContaining("local debug");
    }

    private static ClassLoader markerClassLoader() {
        return new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                if (NoSecurityConfig.LOCAL_DEBUG_MARKER.equals(name)) {
                    return NoSecurityConfigTest.class.getResource("NoSecurityConfigTest.class");
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

    private static void withContextClassLoader(ClassLoader classLoader, ThrowingCallable callable)
            throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
