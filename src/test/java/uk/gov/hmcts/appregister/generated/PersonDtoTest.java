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
import uk.gov.hmcts.appregister.assertion.ConstraintAssertion;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;

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
                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) \\d[A-Z]{2})|(GIR 0A{2}))$\"");
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
                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) \\d[A-Z]{2})|(GIR 0A{2}))$\"");
    }

    @Test
    void testPersonContactDetailsRegexFailure() throws Exception {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setAddressLine1("Test Address Line 1\t");
        contactDetails.setAddressLine2(JsonNullable.of("Test Address Line 2\n"));
        contactDetails.setAddressLine3(JsonNullable.of("Test Address Line 3\r"));
        contactDetails.setAddressLine4(JsonNullable.of("Test Address Line 4\0"));
        contactDetails.setAddressLine5(JsonNullable.of("Test Address Line 5\r\n"));
        contactDetails.setPostcode("AA1 1AA\t");

        contactDetails.setPhone(JsonNullable.of("+01234567890"));
        contactDetails.setMobile(JsonNullable.of("@4472567890"));

        contactDetails.setEmail(JsonNullable.of("test.com"));

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) contactDetails);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(9, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine1",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine2",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine3",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine4",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine5",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "postcode",
                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d)) \\d[A-Z]{2})|(GIR 0A{2}))$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "phone", "must match \"[0-9 \\-]*\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "mobile", "must match \"^(?:\\+\\d{1,4}\\s*)?[0-9 \\-]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "email",
                "must match \"^((([^<>()\\[\\]\\\\.,;:\\s@\"]"
                        + "+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))"
                        + "@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|"
                        + "(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,})))*$\"");
    }

    @Test
    void testPersonContactDetailsPartialFailure() throws Exception {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setAddressLine1("Test Address Line 1\t");
        contactDetails.setAddressLine2(JsonNullable.of("Test Address Line 2"));
        contactDetails.setAddressLine3(JsonNullable.of("Test Address Line 3"));
        contactDetails.setAddressLine4(JsonNullable.of("Test Address Line 4"));
        contactDetails.setAddressLine5(JsonNullable.of("Test Address Line 5\r\n"));
        contactDetails.setPostcode("AA1 1AA\t");

        contactDetails.setPhone(JsonNullable.of("01234567890"));
        contactDetails.setMobile(JsonNullable.of("@4472567890"));

        contactDetails.setEmail(JsonNullable.of("test@test.com"));

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) contactDetails);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        Assertions.assertEquals(4, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine1",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "addressLine5",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "postcode",
                "must match \"^(([A-Z]{1,2}((\\d[A-Z\\d])|(\\d))"
                        + " \\d[A-Z]{2})|(GIR 0A{2}))$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "mobile", "must match \"^(?:\\+\\d{1,4}\\s*)?[0-9 \\-]*$\"");
    }

    @Test
    void testPersonFullNameRegexFailure() throws Exception {
        FullName fullName = new FullName();
        fullName.setFirstForename("Test First Forename\t");
        fullName.setSecondForename(JsonNullable.of("Test Second Forename\n"));
        fullName.setThirdForename(JsonNullable.of("Test Third Forename\r"));
        fullName.setSurname("Test Surname\0");
        fullName.setTitle("Test Title\r\n");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) fullName);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(5, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "firstForename",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "secondForename",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint,
                "thirdForename",
                "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "surname", "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "title", "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
    }

    @Test
    void testPersonFullNamePartialRegexFailure() throws Exception {
        FullName fullName = new FullName();
        fullName.setFirstForename("Test First Forename");
        fullName.setSecondForename(JsonNullable.of("Test Second Forename"));
        fullName.setThirdForename(JsonNullable.of("Test Third Forename"));
        fullName.setSurname("Test Surname\0");
        fullName.setTitle("Test Title");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) fullName);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "surname", "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
    }

    @Test
    void testOrganisationNameRegexFailure() throws Exception {
        Organisation organisation = new Organisation();
        organisation.setContactDetails(new ContactDetails());

        organisation.getContactDetails().setAddressLine1("Test Address Line 1");
        organisation.getContactDetails().setPostcode("AB12 3AA");
        organisation.getContactDetails().setPhone(JsonNullable.of("01234456789"));
        organisation.getContactDetails().setMobile(JsonNullable.of("07123456789"));
        organisation.getContactDetails().setEmail(JsonNullable.of("contact@test.com"));

        organisation.setName("Test Organisation Name\t");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) organisation);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "name", "must match \"^[^\\u0000-\\u001F\\u007F-\\u009F]*$\"");
    }
}
