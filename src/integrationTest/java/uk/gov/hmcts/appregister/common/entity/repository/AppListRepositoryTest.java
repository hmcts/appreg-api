package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
public class AppListRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private UserProvider loggedInUser;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    private static final int BASELINE_TEST_COUNT = 10;

    @Test
    public void testBasicInsertionUpdate() {

        transactionalUnitOfWork.inTransaction(
                () -> {
                    // assert that the save has occurred
                    long count = applicationListRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT, count);

                    // assert
                    // test save
                    ApplicationList listData = new AppListTestData().someComplete();
                    listData = persistance.save(listData);

                    // assert that the save has occurred
                    count = applicationListRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT + 1, count);

                    // test get
                    Optional<ApplicationList> applicationListToAssertAgainst =
                            applicationListRepository.findById(listData.getPk());

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    expectAllCommonEntityFields(listData, applicationListToAssertAgainst.get());
                    assertNotNull(applicationListToAssertAgainst.get());
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    listData.getDate(),
                                    applicationListToAssertAgainst.get().getDate()));
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    listData.getDate(),
                                    applicationListToAssertAgainst.get().getDate()));
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    listData.getTime(),
                                    applicationListToAssertAgainst.get().getTime()));
                    assertEquals(
                            listData.getCourtCode(),
                            applicationListToAssertAgainst.get().getCourtCode());
                    assertEquals(
                            listData.getTime(), applicationListToAssertAgainst.get().getTime());
                    assertEquals(
                            listData.getDurationHours(),
                            applicationListToAssertAgainst.get().getDurationHours());
                    assertEquals(
                            listData.getDurationMinutes(),
                            applicationListToAssertAgainst.get().getDurationMinutes());
                    assertEquals(
                            listData.getOtherLocation(),
                            applicationListToAssertAgainst.get().getOtherLocation());
                    assertEquals(listData.getCja(), applicationListToAssertAgainst.get().getCja());
                });
    }
}
