package uk.gov.hmcts.appregister.common.validator;

import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class DateValidatorTest {
    DateValidatorImpl dateValidator;

    @InjectMocks ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    void setup() {
        dateValidator = new DateValidatorImpl();
        dateValidator.initialize(null);
    }

    @Test
    void testNullDate() {
        Assertions.assertThat(dateValidator.isValid(null, constraintValidatorContext)).isTrue();
    }

    @Test
    void testDateBetween() {
        Assertions.assertThat(dateValidator.isValid(LocalDate.now(), constraintValidatorContext))
                .isTrue();
    }

    @Test
    void testDateAfter() {
        Assertions.assertThat(
                        dateValidator.isValid(LocalDate.of(3001, 1, 1), constraintValidatorContext))
                .isFalse();
    }

    @Test
    void testDateBefore() {
        Assertions.assertThat(
                        dateValidator.isValid(LocalDate.of(1899, 1, 1), constraintValidatorContext))
                .isFalse();
    }

    @Test
    void testDateEqualMinDate() {
        Assertions.assertThat(
                        dateValidator.isValid(LocalDate.of(1900, 1, 1), constraintValidatorContext))
                .isTrue();
    }

    @Test
    void testDateEqualMaxDate() {
        Assertions.assertThat(
                        dateValidator.isValid(
                                LocalDate.of(3000, 12, 31), constraintValidatorContext))
                .isTrue();
    }
}
