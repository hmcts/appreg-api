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
import uk.gov.hmcts.appregister.generated.model.Official;
import utils.ConstraintAssertion;

public class OfficialDtoTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testOfficialEmptyString() throws Exception {
        Official official = new Official();
        official.setForename("");
        official.setSurname("");
        official.setTitle("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) official);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(3, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "forename", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "surname", "size must be between 1 and 100");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "title", "size must be between 1 and 100");
    }
}
