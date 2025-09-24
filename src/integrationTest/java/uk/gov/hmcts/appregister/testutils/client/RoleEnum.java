package uk.gov.hmcts.appregister.testutils.client;

import uk.gov.hmcts.appregister.common.security.RoleNames;

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
}
