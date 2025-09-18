package uk.gov.hmcts.appregister.testutils.client;

public enum RoleEnum {
    ADMIN("Admin"),
    USER("User"),
    NONE("None");
    private final String role;

    RoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
