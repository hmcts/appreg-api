package uk.gov.hmcts.appregister.applicationcode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationcode.exception.AppCodeError;
import uk.gov.hmcts.appregister.applicationcode.validator.ApplicationCodeSortValidator;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

public class ApplicationCodeSortValidatorTest {
    private ApplicationCodeSortValidator validator;

    @BeforeEach
    public void before() {
        validator = new ApplicationCodeSortValidator();
    }

    @Test
    public void testFailureValidation() {
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate("does not exist"));
        Assertions.assertEquals(
                AppCodeError.SORT_NOT_SUITABLE.getCode(), exception.getCode().getCode());
    }

    @Test
    public void testSuccessfulValidation() {
        // if no exception occurs then we succeed the test, no assertions required
        validator.validate(ApplicationCodeSortValidator.VALID_SORT_VALUES[0]);
    }
}
