package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationRegister;
import uk.gov.hmcts.appregister.data.ApplicationRegisterTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

public class ApplicationRegisterRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationRegisterRepository appRegRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    private static final int BASELINE_TEST_COUNT = 0;

    @Test
    public void testBasicInsertionUpdate() {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    ApplicationRegister appRegEntryData =
                            new ApplicationRegisterTestData().someMinimal().build();

                    long count = appRegRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT, count);

                    appRegEntryData = persistance.save(appRegEntryData);

                    // now update
                    // test get
                    Optional<ApplicationRegister> applicationRegToAssertAgainst =
                            appRegRepository.findById(appRegEntryData.getId());

                    // update
                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    expectAllCommonEntityFields(
                            appRegEntryData, applicationRegToAssertAgainst.get());
                    assertEquals(
                            appRegEntryData.getId(), applicationRegToAssertAgainst.get().getId());
                    assertEquals(
                            appRegEntryData.getText(),
                            applicationRegToAssertAgainst.get().getText());
                    assertEquals(
                            appRegEntryData.getApplicationList().getId(),
                            applicationRegToAssertAgainst.get().getApplicationList().getId());
                });
    }
}
