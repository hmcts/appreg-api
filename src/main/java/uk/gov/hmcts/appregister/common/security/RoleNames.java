package uk.gov.hmcts.appregister.common.security;

/** The representation of all accepted role names for authorisation purposes. */
public class RoleNames {
    public static final String ADMIN_ROLE = "Admin";
    public static final String ADMIN_ROLE_RESTRICTION = "hasRole('Admin')";

    public static final String USER_ROLE = "User";
    public static final String USER_ROLE_RESTRICTION = "hasRole('Admin')";

    public static final String USER_ROLE_OR_ADMIN_ROLE_RESTRICTION = "hasAnyRole('Admin', 'User')";
}
