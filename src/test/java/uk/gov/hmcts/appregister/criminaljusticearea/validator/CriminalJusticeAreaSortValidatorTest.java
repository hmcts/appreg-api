package uk.gov.hmcts.appregister.criminaljusticearea.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

class CriminalJusticeAreaSortValidatorTest {
    private CriminalJusticeAreaSortValidator validator;

    @BeforeEach
    void before() {
        validator = new CriminalJusticeAreaSortValidator();
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
        assertDoesNotThrow(() -> validator.validate(CriminalJusticeArea_.CODE));
    }
}
