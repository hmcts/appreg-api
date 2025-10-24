package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationListEntrySortValidatorTest {
    private ApplicationListEntrySortValidator validator;

    @BeforeEach
    void before() {
        validator = new ApplicationListEntrySortValidator();
    }

    @Test
    void testFailureValidation() {
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate("does not exist"));
        Assertions.assertEquals(
                CommonAppError.SORT_NOT_SUITABLE.getCode(), exception.getCode().getCode());
    }

    @Test
    void testSuccessfulValidation() {
        assertDoesNotThrow(() -> validator.validate(ApplicationListEntry_.SEQUENCE_NUMBER));
    }
}
