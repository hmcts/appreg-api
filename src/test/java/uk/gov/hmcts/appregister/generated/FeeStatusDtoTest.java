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
import uk.gov.hmcts.appregister.generated.model.FeeStatus;

public class FeeStatusDtoTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void before() {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
    }

    @Test
    void testOfficialEmptyString() throws Exception {
        FeeStatus status = new FeeStatus();
        status.setPaymentReference("");

        // validate the dto using Bean Validation
        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) status);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        // assert
        Assertions.assertEquals(3, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "paymentReference", "size must be between 1 and 12");
    }
}
