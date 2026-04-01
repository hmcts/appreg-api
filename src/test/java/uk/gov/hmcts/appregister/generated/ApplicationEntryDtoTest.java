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
import uk.gov.hmcts.appregister.assertion.ConstraintAssertion;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;

public class ApplicationEntryDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testEntryCreateDtoEmptyStringErrors() throws Exception {
        // Create an instance of EntryCreateDto
        EntryCreateDto entryCreateDto = new EntryCreateDto();

        // Set properties
        entryCreateDto.setStandardApplicantCode("");
        entryCreateDto.setCaseReference("");
        entryCreateDto.setAccountNumber("");
        entryCreateDto.setNotes("");
        entryCreateDto.setApplicationCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) entryCreateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        Assertions.assertEquals(5, constraintValidator.size());

        // assert
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "accountNumber", "size must be between 1 and 20");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "standardApplicantCode", "size must be between 1 and 10");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "notes", "size must be between 1 and 4000");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "caseReference", "size must be between 1 and 15");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "applicationCode", "size must be between 1 and 10");
    }

    @Test
    void testEntryUpdateDtoEmptyStringErrors() throws Exception {
        // Create an instance of EntryCreateDto
        EntryUpdateDto entryUpdateDto = new EntryUpdateDto();

        // Set properties
        entryUpdateDto.setStandardApplicantCode("");
        entryUpdateDto.setCaseReference("");
        entryUpdateDto.setAccountNumber("");
        entryUpdateDto.setNotes("");
        entryUpdateDto.setApplicationCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) entryUpdateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(6, constraintValidator.size());

        ConstraintAssertion.assertPropertyValue(
                listConstraint, "accountNumber", "size must be between 1 and 20");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "standardApplicantCode", "size must be between 1 and 10");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "notes", "size must be between 1 and 4000");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "caseReference", "size must be between 1 and 15");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "applicationCode", "size must be between 1 and 10");
    }
}
