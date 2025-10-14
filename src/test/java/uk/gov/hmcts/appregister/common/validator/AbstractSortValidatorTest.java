package uk.gov.hmcts.appregister.common.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

class AbstractSortValidatorTest {
    @Test
    void testValidateException() {
        // This is just a placeholder test to ensure the abstract class can be referenced.
        // Actual tests would be in subclasses that implement the abstract methods.
        TestAbstractSortValidator validator = new TestAbstractSortValidator();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate("NOT KNOWN"));
        Assertions.assertEquals(CommonAppError.SORT_NOT_SUITABLE, exception.getCode());
        assertThat(exception.getMessage()).startsWith("Sort property 'NOT KNOWN' is not allowed.");
    }

    @Test
    void testValidate() {
        // This is just a placeholder test to ensure the abstract class can be referenced.
        // Actual tests would be in subclasses that implement the abstract methods.
        TestAbstractSortValidator validator = new TestAbstractSortValidator();
        validator.validate("field1");
    }

    static class TestAbstractSortValidator extends AbstractSortValidator {
        TestAbstractSortValidator() {
            super("field1", "field2");
        }
    }
}
