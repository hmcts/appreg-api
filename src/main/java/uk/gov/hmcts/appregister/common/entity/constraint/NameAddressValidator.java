package uk.gov.hmcts.appregister.common.entity.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

/**
 * Let us ensure that when we insert a name address, we ensure:- If an organisation representation,
 * then fields that are specific to a person are not populated.
 */
public class NameAddressValidator implements ConstraintValidator<ValidNameAddress, NameAddress> {

    @SuppressWarnings("checkstyle:NeedBraces")
    @Override
    public boolean isValid(NameAddress nameAddress, ConstraintValidatorContext context) {
        if (nameAddress.getName() != null
                && nameAddress.getTitle() == null
                && nameAddress.getForename1() == null
                && nameAddress.getForename2() == null
                && nameAddress.getForename3() == null
                && nameAddress.getSurname() == null) {
            return true;
        } else return nameAddress.getName() == null;
    }
}
