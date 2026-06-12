package uk.gov.hmcts.appregister.common.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ValidatorCoverageTest {

    @Test
    void validate_defaultMethodInvokesBaseValidationAndCallback() {
        var validated = new AtomicBoolean();
        Validator<String, Object> validator =
                validatable -> {
                    assertEquals("input", validatable);
                    validated.set(true);
                };

        var result =
                validator.validate(
                        "input",
                        (validatable, success) -> {
                            assertEquals("input", validatable);
                            assertNull(success);
                            return "done";
                        });

        assertTrue(validated.get());
        assertEquals("done", result);
        assertEquals(1, Validator.SINGLE_RECORD);
    }
}
