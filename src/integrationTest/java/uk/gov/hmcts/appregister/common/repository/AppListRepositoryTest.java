package uk.gov.hmcts.appregister.common.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;
import uk.gov.hmcts.appregister.testutils.DateUtil;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.data.AppListData;
import uk.gov.hmcts.appregister.testutils.data.AppListEntryData;

@Slf4j
public class AppListRepositoryTest extends BasePostgresIntegrationTest {

    @Autowired private ApplicationListRepository applicationListRepository;

    @Autowired private UserProvider loggedInUser;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    private static final int BASELINE_TEST_COUNT = 10;

    @Test
    public void testBasicInsertionUpdate() throws Exception {

        transactionalUnitOfWork.inTransaction(
                () -> {
                    // assert that the save has occurred
                    long count = applicationListRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT, count);

                    // assert
                    // test save
                    ApplicationList listData = new AppListData().someMinimal().build();
                    ApplicationListEntry entry = new AppListEntryData().someMinimal().build();
                    listData.setEntries(new ArrayList<>());
                    listData.getEntries().add(entry);

                    ApplicationList data = persistance.save(listData);

                    // assert that the save has occurred
                    count = applicationListRepository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT + 1, count);

                    // test get
                    Optional<ApplicationList> applicationListToAssertAgainst =
                            applicationListRepository.findById(data.getId());

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    expectAllCommonEntityFields(listData, applicationListToAssertAgainst);
                    assertNotNull(applicationListToAssertAgainst.get());
                    assertEquals(
                            data.getEntries().getFirst().getId(),
                            applicationListToAssertAgainst.get().getEntries().getFirst().getId());
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    data.getDate(),
                                    applicationListToAssertAgainst.get().getDate()));
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    data.getDate(),
                                    applicationListToAssertAgainst.get().getDate()));
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    data.getTime(),
                                    applicationListToAssertAgainst.get().getTime()));
                    assertEquals(
                            data.getListDescription(),
                            applicationListToAssertAgainst.get().getListDescription());
                });
    }
}
