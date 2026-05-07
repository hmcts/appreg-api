package uk.gov.hmcts.appregister.common.entity.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameAddressValidator.class)
@Documented
public @interface ValidNameAddress {

    String message() default "name address is not valid according to business rules";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
