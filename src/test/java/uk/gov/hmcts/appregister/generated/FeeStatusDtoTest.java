package uk.gov.hmcts.appregister.generated;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.FeeStatus;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;
import utils.ConstraintAssertion;

class FeeStatusDtoTest {
    private static final LocalDate STATUS_DATE = LocalDate.of(2025, 1, 2);

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
                listConstraint, "paymentReference", "size must be between 1 and 15");
    }

    @Test
    void givenPaymentReferenceLongerThanFifteenCharacters_whenValidating_thenConstraintViolation()
            throws Exception {
        FeeStatus status = new FeeStatus();
        status.setPaymentReference("1234512345123456");
        status.setPaymentStatus(PaymentStatus.PAID);
        status.setStatusDate(STATUS_DATE);

        Set<ConstraintViolation<Object>> constraintValidator =
                Validation.byDefaultProvider()
                        .configure()
                        .buildValidatorFactory()
                        .getValidator()
                        .validate((Object) status);

        List<ConstraintViolation<Object>> listConstraint = constraintValidator.stream().toList();

        Assertions.assertEquals(1, constraintValidator.size());
        ConstraintAssertion.assertPropertyValue(
                listConstraint, "paymentReference", "size must be between 1 and 15");
    }
}
