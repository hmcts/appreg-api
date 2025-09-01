package uk.gov.hmcts.appregister.testutils.stubs;

public enum RoleEnum {
    ADMIN("Admin");
    private final String role;

    RoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
