package uk.gov.hmcts.appregister.common.validator;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.validator.regex.DisallowedCharactersRegexValidator;

class DisallowedCharactersRegexValidatorTest {

    private final DisallowedCharactersRegexValidator validator =
            new DisallowedCharactersRegexValidator();

    @Test
    public void testShouldReturnTrueForValidInput() {
        String input = "ValidInput123";
        String field = "testField";
        Map<String, String> validatable = Map.of("value", input, "field", field);
        Assertions.assertDoesNotThrow(() -> validator.validate(validatable));
    }

    @Test
    public void testInvalidValueShouldThrowAppRegistryException() {
        String input = "Invalid$Input";
        String field = "testField";
        Map<String, String> validatable = Map.of("value", input, "field", field);
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(validatable));
        Assertions.assertEquals("Regex Validation Mismatch", exception.getMessage());
        Assertions.assertEquals(validatable, exception.getDetails());
        Assertions.assertEquals(
                CommonAppError.REGEX_VALIDATION_MISMATCH_ERROR, exception.getCode());
    }

    @Test
    public void testShouldReturnTrueForEmptyString() {
        String input = "";
        String field = "testField";
        Map<String, String> validatable = Map.of("value", input, "field", field);
        Assertions.assertDoesNotThrow(() -> validator.validate(validatable));
    }

    @Test
    public void testShouldReturnTrueForNullInput() {
        String field = "testField";
        Map<String, String> validatable = new HashMap<>();
        validatable.put("field", field);
        validatable.put("value", null);

        Assertions.assertDoesNotThrow(() -> validator.validate(validatable));
    }
}
