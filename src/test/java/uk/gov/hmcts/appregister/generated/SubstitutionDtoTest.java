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
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public class SubstitutionDtoTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testSubstitutionEmptyString() throws Exception {
        TemplateSubstitution substitution = new TemplateSubstitution();
        substitution.setValue("");
        substitution.setKey("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) substitution);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(2, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "value", "size must be between 1 and 2147483647");
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "key", "size must be between 1 and 2147483647");
    }
}
