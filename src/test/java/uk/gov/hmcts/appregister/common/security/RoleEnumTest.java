package uk.gov.hmcts.appregister.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleEnumTest {

    @Test
    void getRole_returnsExpectedRoleName() {
        assertThat(RoleEnum.ADMIN.getRole()).isEqualTo(RoleNames.ADMIN_ROLE);
        assertThat(RoleEnum.USER.getRole()).isEqualTo(RoleNames.USER_ROLE);
        assertThat(RoleEnum.NONE.getRole()).isEqualTo("None");
    }

    @Test
    void isAdmin_returnsTrueWhenAdminRolePresent() {
        assertThat(RoleEnum.isAdmin("other", RoleNames.ADMIN_ROLE)).isTrue();
    }

    @Test
    void isAdmin_returnsFalseWhenRolesMissingOrNull() {
        assertThat(RoleEnum.isAdmin("other", RoleNames.USER_ROLE)).isFalse();
        assertThat(RoleEnum.isAdmin((String[]) null)).isFalse();
    }

    @Test
    void isUser_returnsTrueWhenUserRolePresent() {
        assertThat(RoleEnum.isUser("other", RoleNames.USER_ROLE)).isTrue();
    }

    @Test
    void isUser_returnsFalseWhenRolesMissingOrNull() {
        assertThat(RoleEnum.isUser("other", RoleNames.ADMIN_ROLE)).isFalse();
        assertThat(RoleEnum.isUser((String[]) null)).isFalse();
    }
}
