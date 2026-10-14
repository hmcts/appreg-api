package uk.gov.hmcts.appregister.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DateValidatorImpl.class)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface DateValidator {
    String message() default "Invalid date, date is out of range (1900-01-01 to 3000-12-31)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
