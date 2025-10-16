package uk.gov.hmcts.appregister.resultcode.validatior;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.resultcode.api.ResultCodeApiFields;
import uk.gov.hmcts.appregister.resultcode.validator.ResultCodeSortValidator;

public class ResultCodeSortValidatorTest {

    private ResultCodeSortValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ResultCodeSortValidator();
    }

    @Test
    void allowsCode() {
        assertDoesNotThrow(() -> validator.validate(ResultCodeApiFields.CODE));
    }

    @Test
    void allowsTitle() {
        assertDoesNotThrow(() -> validator.validate(ResultCodeApiFields.TITLE));
    }

    @Test
    void rejects_unknownProperty() {
        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class, () -> validator.validate("notARealProperty"));
        Assertions.assertTrue(
                ex.getMessage().contains("Sort property 'notARealProperty' is not allowed"),
                "Exception message should mention disallowed property");
    }

    @Test
    void rejects_null() {
        assertThrows(AppRegistryException.class, () -> validator.validate(null));
    }

    @Test
    void rejects_empty() {
        assertThrows(AppRegistryException.class, () -> validator.validate(""));
    }

    @Test
    void rejects_whitespaceOnly() {
        assertThrows(AppRegistryException.class, () -> validator.validate("   "));
    }

    @Test
    void trims_inputButStillCaseSensitive() {
        // Leading/trailing spaces are trimmed, so exact property with spaces is OK
        assertDoesNotThrow(() -> validator.validate(" " + ResultCodeApiFields.CODE + " "));

        // Case sensitivity check: upper-case version should fail
        assertThrows(
                AppRegistryException.class,
                () -> validator.validate(ResultCodeApiFields.CODE.toUpperCase()));
    }
}
