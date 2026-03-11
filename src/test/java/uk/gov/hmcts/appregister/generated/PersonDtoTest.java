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
import org.openapitools.jackson.nullable.JsonNullable;
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
        JsonNullable<String> emptyNullable = JsonNullable.of("");
        FullName fullName = new FullName();
        fullName.setSurname("");
        fullName.setFirstForename("");
        fullName.setTitle("");
        fullName.setThirdForename(emptyNullable);
        fullName.setSecondForename(emptyNullable);

        Person person = new Person();
        person.setName(fullName);

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmail(emptyNullable);
        contactDetails.setPostcode("");
        contactDetails.setAddressLine1("");
        contactDetails.setAddressLine2(emptyNullable);
        contactDetails.setAddressLine3(emptyNullable);
        contactDetails.setAddressLine4(emptyNullable);
        contactDetails.setAddressLine5(emptyNullable);

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
        Assertions.assertEquals(13, constraintValidator.size());
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
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.postcode", "size must be between 1 and 8");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "contactDetails.postcode",
                "must match \"^([A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}|GIR ?0A{2})$\"");
    }

    @Test
    void testOrganisationEmptyStrings() throws Exception {
        JsonNullable<String> emptyNullable = JsonNullable.of("");
        Organisation organisation = new Organisation();
        organisation.setName("");

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setEmail(emptyNullable);
        contactDetails.setPostcode("");
        contactDetails.setAddressLine1("");
        contactDetails.setAddressLine2(emptyNullable);
        contactDetails.setAddressLine3(emptyNullable);
        contactDetails.setAddressLine4(emptyNullable);
        contactDetails.setAddressLine5(emptyNullable);

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
        Assertions.assertEquals(9, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.addressLine4", "size must be between 1 and 35");
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
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "contactDetails.postcode", "size must be between 1 and 8");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "contactDetails.postcode",
                "must match \"^([A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}|GIR ?0A{2})$\"");
    }
}
