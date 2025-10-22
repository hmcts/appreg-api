package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;

import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.util.DateUtil;

public class AppListEntryRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @PersistenceContext private EntityManager entityManager;

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

    @Test
    public void testFindSummariesById() {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    StandardApplicant standardApplicant =
                        new StandardApplicantTestData()
                            .someComplete();

                    NameAddress nameAddress =
                        new NameAddressTestData()
                            .someComplete();

                    ResolutionCode resolutionCode =
                        new ResolutionCodeTestData()
                            .someComplete();
                    entityManager.persist(resolutionCode);
                    entityManager.flush();

                    ApplicationListEntry listEntryData =
                        new AppListEntryTestData()
                            .someMinimal()
                            .accountNumber("1234567890")
                            .standardApplicant(standardApplicant)
                            .rnameaddress(nameAddress)
                            .build();

                    AppListEntryResolution appListEntryResolution =
                        new AppListEntryResolutionTestData()
                            .someMinimal()
                            .applicationList(listEntryData)
                            .resolutionCode(resolutionCode)
                            .build();
                    List<AppListEntryResolution> resolutions = List.of(appListEntryResolution);
                    listEntryData.setResolutions(resolutions);

                    ApplicationListEntry data = persistance.save(listEntryData);

                    for (AppListEntryResolution resolution : resolutions) {
                        entityManager.persist(resolution);
                    }
                    entityManager.flush();

                    // test get
                    Pageable page = PageRequest.of(0, 10);
                    Page<ApplicationListEntrySummaryProjection> applicationListEntrySummaryProjectionsToAssertAgainst =
                        applicationListEntryRepository.findSummariesById(data.getApplicationList().getUuid(), page);

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    assertNotNull(applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst());
                    assertEquals(
                        listEntryData.getSequenceNumber(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst()
                            .getSequenceNumber());
                    assertEquals(
                        listEntryData.getAccountNumber(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst()
                            .getAccountNumber());
                    assertEquals(
                        listEntryData.getStandardApplicant().getName(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst().getApplicant());
                    assertEquals(
                        listEntryData.getRnameaddress().getName(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst().getRespondent());
                    assertEquals(
                        listEntryData.getRnameaddress().getPostcode(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst().getPostCode());
                    assertEquals(
                        listEntryData.getApplicationCode().getTitle(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst()
                            .getApplicationTitle());
                    assertEquals(
                        listEntryData.getResolutions().getFirst().getResolutionCode().getResultCode(),
                        applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst().getResult());
                    assertThat(
                        applicationListEntrySummaryProjectionsToAssertAgainst.getTotalElements()).isEqualTo(1);
                });
    }
}
