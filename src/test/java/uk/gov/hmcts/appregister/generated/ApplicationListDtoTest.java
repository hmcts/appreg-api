package uk.gov.hmcts.appregister.generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.assertion.ConstraintAssertion;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

public class ApplicationListDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testListCreateDtoEmptyStringErrors() throws Exception {
        // Create an instance of EntryCreateDto
        ApplicationListCreateDto applicationListDto = new ApplicationListCreateDto();

        // Set properties
        applicationListDto.setOtherLocationDescription("");
        applicationListDto.setCjaCode("");
        applicationListDto.setCourtLocationCode("");
        applicationListDto.setDescription("");

        applicationListDto.setTime(LocalTime.now());

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) applicationListDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        for (ConstraintViolation<Object> constraintViolation : listConstraint) {
            System.out.println(
                    constraintViolation.getPropertyPath() + " " + constraintViolation.getMessage());
        }

        Assertions.assertEquals(6, constraintValidator.size());

        ConstraintAssertion.assertPropertyValue(
                listConstraint, "courtLocationCode", "size must be between 1 and 10");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "cjaCode", "size must be between 1 and 2");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "description", "size must be between 1 and 200");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "otherLocationDescription", "size must be between 1 and 200");
    }

    @Test
    void testListUpdateDtoEmptyStringErrors() throws Exception {
        ApplicationListUpdateDto applicationListDto = new ApplicationListUpdateDto();

        // Set properties
        applicationListDto.setOtherLocationDescription("");
        applicationListDto.setCjaCode("");
        applicationListDto.setCourtLocationCode("");
        applicationListDto.setDescription("");

        applicationListDto.setTime(LocalTime.now());

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) applicationListDto);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(6, constraintValidator.size());

        ConstraintAssertion.assertPropertyValue(
                listConstraint, "courtLocationCode", "size must be between 1 and 10");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "cjaCode", "size must be between 1 and 2");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "description", "size must be between 1 and 200");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "otherLocationDescription", "size must be between 1 and 200");
    }
}
