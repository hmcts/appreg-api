package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Unit tests for {@link ApplicationListSortValidator}.
 */
class ApplicationListSortValidatorTest {

    private ApplicationListSortValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ApplicationListSortValidator();
    }

    @Test
    void validate_allowedProperty_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validate(ApplicationList_.DATE));
        assertDoesNotThrow(() -> validator.validate(ApplicationList_.TIME));
        assertDoesNotThrow(() -> validator.validate(ApplicationList_.STATUS));
        assertDoesNotThrow(() -> validator.validate(ApplicationList_.DESCRIPTION));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"notAValidField", "  "})
    void validate_invalidProperty_throwsAppRegistryException(String sortProperty) {
        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> validator.validate(sortProperty));
        assertThat(ex.getMessage()).contains("not allowed");
    }
}
