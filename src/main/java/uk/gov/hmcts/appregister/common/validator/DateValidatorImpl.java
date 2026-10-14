package uk.gov.hmcts.appregister.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class DateValidatorImpl implements ConstraintValidator<DateValidator, LocalDate> {
    private LocalDate minDate;
    private LocalDate maxDate;

    @Override
    public void initialize(DateValidator constraintAnnotation) {
        minDate = LocalDate.of(1900, 1, 1);
        maxDate = LocalDate.of(3000, 12, 31);
    }

    @Override
    public boolean isValid(
            LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        if (localDate == null) {
            return true;
        }

        return localDate.isAfter(minDate.minusDays(1)) && localDate.isBefore(maxDate.plusDays(1));
    }
}
