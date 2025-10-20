package uk.gov.hmcts.appregister.common.security;

public enum RoleEnum {
    ADMIN(RoleNames.ADMIN_ROLE),
    USER(RoleNames.USER_ROLE),
    NONE("None");
    private final String role;

    RoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public static boolean isAdmin(String... roles) {
        if (roles != null) {
            for (String role : roles) {
                if (ADMIN.getRole().equals(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isUser(String... roles) {
        if (roles != null) {
            for (String role : roles) {
                if (USER.getRole().equals(role)) {
                    return true;
                }
            }
        }

        return false;
    }
}
