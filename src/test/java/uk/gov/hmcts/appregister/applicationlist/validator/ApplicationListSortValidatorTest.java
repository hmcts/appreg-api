package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Unit tests for {@link ApplicationListSortValidator}.
 */
public class ApplicationListSortValidatorTest {

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

    @Test
    void validate_disallowedProperty_throwsAppRegistryException() {
        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class, () -> validator.validate("notAValidField"));
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void validate_nullProperty_throwsAppRegistryException() {
        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void validate_blankProperty_throwsAppRegistryException() {
        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> validator.validate("  "));
        assertTrue(ex.getMessage().contains("not allowed"));
    }
}
