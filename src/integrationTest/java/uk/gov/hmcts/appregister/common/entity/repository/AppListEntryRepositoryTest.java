package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.util.DateUtil;

public class AppListEntryRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @Test
    public void testBasicInsertionUpdate() {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    // assert
                    // test save
                    ApplicationListEntry listEntryData =
                            new AppListEntryTestData().someMinimal().build();

                    ApplicationListEntry data = persistance.save(listEntryData);

                    // test get
                    Optional<ApplicationListEntry> applicationListEntryToAssertAgainst =
                            applicationListEntryRepository.findById(data.getId());

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    expectAllCommonEntityFields(
                            listEntryData, applicationListEntryToAssertAgainst.get());
                    assertNotNull(applicationListEntryToAssertAgainst.get());
                    assertEquals(
                            listEntryData.getApplicationCode().getId(),
                            applicationListEntryToAssertAgainst.get().getApplicationCode().getId());
                    assertTrue(
                            DateUtil.equalsIgnoreMillis(
                                    listEntryData.getLodgementDate(),
                                    applicationListEntryToAssertAgainst.get().getLodgementDate()));
                    assertEquals(
                            listEntryData.getSequenceNumber(),
                            applicationListEntryToAssertAgainst.get().getSequenceNumber());
                    assertEquals(
                            listEntryData.getEntryRescheduled(),
                            applicationListEntryToAssertAgainst.get().getEntryRescheduled());
                    assertEquals(
                            listEntryData.getApplicationListEntryWording(),
                            applicationListEntryToAssertAgainst
                                    .get()
                                    .getApplicationListEntryWording());
                });
    }
}
