package uk.gov.hmcts.appregister.config;

/**
 * Thrown when the local-only {@code nosecurity} profile is configured unsafely.
 */
public class NoSecurityConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoSecurityConfigurationException(String message) {
        super(message);
    }

    public NoSecurityConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
