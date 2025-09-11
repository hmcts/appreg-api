package uk.gov.hmcts.appregister.common.security;

/** The representation of all accepted role names for authorisation purposes. */
public class RoleNames {
    public static final String ADMIN_ROLE = "Admin";
    public static final String ADMIN_ROLE_RESTRICTION = "hasRole('Admin')";
}
