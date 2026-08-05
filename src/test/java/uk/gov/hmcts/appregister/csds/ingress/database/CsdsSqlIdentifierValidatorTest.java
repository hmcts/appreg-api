package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CsdsSqlIdentifierValidatorTest {
    @Test
    void given_uppercaseIdentifier_when_validate_then_acceptIt() {
        assertThat(CsdsSqlIdentifierValidator.requireValid("AppReg_SCHEMA1", "schema"))
                .isEqualTo("AppReg_SCHEMA1");
    }

    @Test
    void given_invalidIdentifier_when_validate_then_rejectIt() {
        assertThatThrownBy(() -> CsdsSqlIdentifierValidator.requireValid("appreg-schema", "schema"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL schema");
    }
}
