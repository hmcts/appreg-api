package uk.gov.hmcts.appregister.config;

/**
 * Security-related constants used across the application.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // Claim names
    public static final String ROLE_CLAIM = "roles";

    // Authority prefix
    public static final String ROLE_PREFIX = "ROLE_";

    // Endpoint patterns
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String OPENAPI_DOCS = "/v3/api-docs/**";
    public static final String OPENAPI = "/specs/**";
    public static final String HEALTH = "/health/**";
    public static final String REST_IMPLEMENTATION_HEALTH = "/implementation-rest-status/**";

    // Error codes
    public static final int ERR_AUTH_REQUIRED = 401;
    public static final int ERR_FORBIDDEN = 403;
}
