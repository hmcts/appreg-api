package uk.gov.hmcts.appregister.generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import utils.ConstraintAssertion;

public class PersonDtoTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testPersonEmptyStrings() throws Exception {
        FullName fullName = new FullName();
        fullName.setSurname("");
        fullName.setFirstForename("");
        fullName.setTitle("");
        fullName.setThirdForename("");
        fullName.setSecondForename("");

        Person person = new Person();
        person.setName(fullName);

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmail("");
        contactDetails.setPostcode("");
        contactDetails.setAddressLine1("");
        contactDetails.setAddressLine2("");
        contactDetails.setAddressLine3("");
        contactDetails.setAddressLine4("");
        contactDetails.setAddressLine5("");

        person.setContactDetails(contactDetails);

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) person);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(12, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.surname", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.secondForename", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.firstForename", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine4", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.thirdForename", "size must be between 1 and 100");

        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.postcode", "size must be between 1 and 8");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.email", "size must be between 1 and 253");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.surname", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name.title", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine1", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine2", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine5", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine3", "size must be between 1 and 35");
    }

    @Test
    void testOrganisationEmptyStrings() throws Exception {
        Organisation organisation = new Organisation();
        organisation.setName("");

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmail("");
        contactDetails.setPostcode("");
        contactDetails.setAddressLine1("");
        contactDetails.setAddressLine2("");
        contactDetails.setAddressLine3("");
        contactDetails.setAddressLine4("");
        contactDetails.setAddressLine5("");

        organisation.setContactDetails(contactDetails);

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) organisation);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(8, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine4", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.postcode", "size must be between 1 and 8");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.email", "size must be between 1 and 253");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine1", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine2", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine5", "size must be between 1 and 35");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine3", "size must be between 1 and 35");
    }
}
