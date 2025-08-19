package uk.gov.hmcts.appregister.config;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // Claim names
    public static final String CLAIM_ROLES = "roles";

    // Authority prefix
    public static final String ROLE_PREFIX = "ROLE_";

    // Role values
    public static final String ADMIN_ROLE = "Admin";
    public static final String USER_ROLE = "User";

    // Endpoint patterns
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String OPENAPI_DOCS = "/v3/api-docs/**";
    public static final String HEALTH = "/health/**";
    public static final String ADMIN_ENDPOINT = "/admin/**";
    public static final String USER_ENDPOINT = "/user/**";

    // Error messages
    public static final String AUD_MISMATCH_MESSAGE =
            "The access token was not issued for this API (audience mismatch).";

    // Error codes
    public static final int ERR_AUTH_REQUIRED = 401;
    public static final int ERR_FORBIDDEN = 403;
    public static final String INVALID_TOKEN = "invalid_token";
}
