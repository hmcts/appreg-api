package utils;

import jakarta.validation.ConstraintViolation;
import java.util.List;
import org.junit.jupiter.api.Assertions;

/**
 * A class that allows validation constraints to be asserted in a consistent way across tests.
 */
public class ConstraintAssertion {
    /**
     * Asserts a constraint name and value using a list of constraint violations.
     *
     * @param listConstraint the list of constraint violations to check
     * @param propertyName the name of the property to check
     * @param propertyValue the expected value of the constraint violation message
     */
    public static void assertPropertyValue(
            List<ConstraintViolation<Object>> listConstraint,
            String propertyName,
            String propertyValue) {
        List<ConstraintViolation<Object>> constraintViolation =
                listConstraint.stream()
                        .filter(c -> c.getPropertyPath().toString().equals(propertyName))
                        .toList();

        // assert
        Assertions.assertNotNull(constraintViolation);
        Assertions.assertTrue(constraintViolation.size() > 0);

        // this handles more than one violation for the same property, it checks that at least one
        // of the violations has the expected message
        Assertions.assertEquals(
                propertyValue,
                constraintViolation.stream()
                        .filter(o -> o.getMessage().equals(propertyValue))
                        .findFirst()
                        .orElseThrow()
                        .getMessage());
    }
}
