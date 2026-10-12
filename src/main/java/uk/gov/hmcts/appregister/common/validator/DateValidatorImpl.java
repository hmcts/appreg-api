package uk.gov.hmcts.appregister.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DateValidatorImpl implements ConstraintValidator<DateValidator, LocalDate> {

    @Value("${appreg.date-validation.min-date}")
    private String minDateString;

    @Value("${appreg.date-validation.max-date}")
    private String maxDateString;

    private LocalDate minDate;
    private LocalDate maxDate;

    @Override
    public void initialize(DateValidator constraintAnnotation) {
        // Parse the string values to LocalDate during initialization
        this.minDate = LocalDate.parse(minDateString);
        this.maxDate = LocalDate.parse(maxDateString);
    }

    @Override
    public boolean isValid(
            LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        return localDate.isAfter(minDate) && localDate.isBefore(maxDate);
    }
}
