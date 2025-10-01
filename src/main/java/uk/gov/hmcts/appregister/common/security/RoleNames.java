package uk.gov.hmcts.appregister.common.security;

/**
 * The representation of all accepted role names for authorisation purposes.
 */
public class RoleNames {
    public static final String ADMIN_ROLE = "admin";
    public static final String ADMIN_ROLE_RESTRICTION = "hasRole('admin')";

    public static final String USER_ROLE = "user";
    public static final String USER_ROLE_RESTRICTION = "hasRole('user')";

    public static final String USER_ROLE_OR_ADMIN_ROLE_RESTRICTION = "hasAnyRole('admin', 'user')";
}
