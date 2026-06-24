package uk.gov.hmcts.appregister.common.entity.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

/**
 * Let us ensure that when we insert a name address, we ensure:- If an organisation representation,
 * then fields that are specific to a person are not populated.
 */
public class NameAddressValidator implements ConstraintValidator<ValidNameAddress, NameAddress> {

    @Override
    public boolean isValid(NameAddress nameAddress, ConstraintValidatorContext context) {
        return nameAddress == null
                || nameAddress.getName() == null
                || (nameAddress.getTitle() == null
                        && nameAddress.getFirstName() == null
                        && nameAddress.getMiddleName() == null
                        && nameAddress.getLastName() == null);
    }
}
