package uk.gov.hmcts.appregister.common.entity.repository.constraint;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionSystemException;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.NameAddressRepository;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

/**
 * Tests the database constraints for {@link uk.gov.hmcts.appregister.common.entity.NameAddress}.
 * See {@link uk.gov.hmcts.appregister.common.entity.constraint.NameAddressValidator}
 */
public class NameAddressConstraintTest extends BaseRepositoryTest {
    @Autowired private NameAddressRepository nameAddressRepository;

    @Test
    public void testOrganisationWithForenameFailure() {
        NameAddress nameAddress = new NameAddressTestData().someComplete();
        nameAddress.setName("Test Name");
        nameAddress.setForename1("Test Forename");
        TransactionSystemException rollbackException =
                Assertions.assertThrows(
                        TransactionSystemException.class,
                        () -> {
                            nameAddressRepository.save(nameAddress);
                        });
        Assertions.assertEquals(
                "name address is not valid according to business rules",
                ((ConstraintViolationException) rollbackException.getCause().getCause())
                        .getConstraintViolations().stream().findFirst().get().getMessage());
    }
}
