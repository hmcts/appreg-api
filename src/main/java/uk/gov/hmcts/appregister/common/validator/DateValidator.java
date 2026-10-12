package uk.gov.hmcts.appregister.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DateValidatorImpl.class)
@Target({ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface DateValidator {
    String message() default "Date must be between {minDate} and {maxDate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
