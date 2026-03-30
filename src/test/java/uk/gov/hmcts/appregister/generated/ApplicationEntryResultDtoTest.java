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
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

public class ApplicationEntryResultDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testEntryResultCreateDtoEmptyStringErrors() throws Exception {
        // Create an instance of EntryCreateDto
        uk.gov.hmcts.appregister.generated.model.ResultCreateDto resultCreateDto =
                new ResultCreateDto();

        // Set properties
        resultCreateDto.setResultCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) resultCreateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "resultCode", "size must be between 1 and 10");
    }

    @Test
    void testEntryResultUpdateDtoEmptyStringErrors() throws Exception {
        uk.gov.hmcts.appregister.generated.model.ResultUpdateDto resultUpdateDto =
                new ResultUpdateDto();

        // Set properties
        resultUpdateDto.setResultCode("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) resultUpdateDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "resultCode", "size must be between 1 and 10");
    }
}
