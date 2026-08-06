package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

class CriminalJusticeRepositoryTest extends BaseRepositoryTest {

    @Autowired private CriminalJusticeAreaRepository criminalJusticeAreaRepository;

    @Autowired private UserProvider loggedInUser;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    // Count for the baseline integration test data in the flyway script.
    private static final int BASELINE_TEST_COUNT = 4;

    @Test
    void testBasicInsertionUpdate() throws Exception {

        transactionalUnitOfWork.inTransaction(
                () -> {
                    // assert that the save has occurred
                    long count = criminalJusticeAreaRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT, count);

                    // assert
                    // test save
                    CriminalJusticeArea dataToPersist =
                            new CriminalJusticeTestData().someMinimal().build();
                    dataToPersist.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
                    CriminalJusticeArea data = persistance.save(dataToPersist);

                    // assert that the save has occurred
                    count = criminalJusticeAreaRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT + 1, count);

                    // test get
                    Optional<CriminalJusticeArea> criminalJusticeAreaToAssertAgainst =
                            criminalJusticeAreaRepository.findById(data.getId());

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    assertThat(criminalJusticeAreaToAssertAgainst).isNotEmpty();
                    expectAllCommonEntityFields(
                            dataToPersist, criminalJusticeAreaToAssertAgainst.get());
                    assertEquals(
                            dataToPersist.getCode(),
                            criminalJusticeAreaToAssertAgainst.get().getCode());
                    assertEquals(
                            dataToPersist.getDescription(),
                            criminalJusticeAreaToAssertAgainst.get().getDescription());
                });
    }
}
