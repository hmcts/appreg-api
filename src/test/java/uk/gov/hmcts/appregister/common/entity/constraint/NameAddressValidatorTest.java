package uk.gov.hmcts.appregister.common.entity.constraint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NameAddress;

class NameAddressValidatorTest {

    private final NameAddressValidator validator = new NameAddressValidator();

    @Test
    void isValid_returnsTrueForNullNameAddress() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void isValid_returnsTrueForOrganisationWithOnlyOrganisationFields() {
        var nameAddress = new NameAddress();
        nameAddress.setName("Org Ltd");

        assertTrue(validator.isValid(nameAddress, null));
    }

    @Test
    void isValid_returnsFalseForOrganisationWithPersonFields() {
        var nameAddress = new NameAddress();
        nameAddress.setName("Org Ltd");
        nameAddress.setFirstName("Ada");

        assertFalse(validator.isValid(nameAddress, null));
    }

    @Test
    void isValid_returnsTrueForPersonWithoutOrganisationName() {
        var nameAddress = new NameAddress();
        nameAddress.setFirstName("Ada");
        nameAddress.setLastName("Lovelace");

        assertTrue(validator.isValid(nameAddress, null));
    }
}
