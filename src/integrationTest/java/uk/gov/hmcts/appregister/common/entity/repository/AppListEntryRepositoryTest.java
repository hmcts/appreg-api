package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryUtil;
import uk.gov.hmcts.appregister.util.DateUtil;

@Transactional
@Rollback
public class AppListEntryRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @PersistenceContext private EntityManager entityManager;

    @Test
    public void testBasicInsertionUpdate() {
        // assert
        // test save
        ApplicationListEntry listEntryData = new AppListEntryTestData().someMinimal().build();

        ApplicationListEntry data = persistance.save(listEntryData);

        // test get
        Optional<ApplicationListEntry> applicationListEntryToAssertAgainst =
                applicationListEntryRepository.findById(data.getId());

        // assert that the data that has been retrieved aligns with the data that we
        // have stored
        expectAllCommonEntityFields(listEntryData, applicationListEntryToAssertAgainst.get());
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
                applicationListEntryToAssertAgainst.get().getApplicationListEntryWording());
    }

    @Test
    public void testFindSummariesById_returnsExpectedSummaryProjection() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry data =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 1);

        // test get
        Pageable page = PageRequest.of(0, 10);
        Page<ApplicationListEntrySummaryProjection>
                applicationListEntrySummaryProjectionsToAssertAgainst =
                        applicationListEntryRepository.findSummariesById(
                                data.getApplicationList().getUuid(), page);

        // assert that the data that has been retrieved aligns with the data that we
        // have stored
        assertNotNull(
                applicationListEntrySummaryProjectionsToAssertAgainst.getContent().getFirst());
        assertEquals(
                data.getSequenceNumber(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getSequenceNumber());
        assertEquals(
                data.getAccountNumber(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getAccountNumber());
        assertEquals(
                data.getStandardApplicant().getName(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getApplicant());
        assertEquals(
                data.getRnameaddress().getName(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getRespondent());
        assertEquals(
                data.getRnameaddress().getPostcode(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getPostCode());
        assertEquals(
                data.getApplicationCode().getTitle(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getApplicationTitle());
        assertEquals(
                data.getResolutions().getFirst().getResolutionCode().getResultCode(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getResult());
        assertThat(applicationListEntrySummaryProjectionsToAssertAgainst.getTotalElements())
                .isEqualTo(1);
    }

    @Test
    public void testFindSummariesById_paging_and_sorting() {
        // Given
        ApplicationList list = new AppListTestData().someMinimal().build();
        Short sequenceNumber1 = (short) 1;
        Short sequenceNumber2 = (short) 2;
        ApplicationListEntry data1 =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, sequenceNumber1);
        ApplicationListEntry data2 =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, sequenceNumber2);

        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 1);
        Page<ApplicationListEntrySummaryProjection> page0 =
                applicationListEntryRepository.findSummariesById(
                        data1.getApplicationList().getUuid(), page);

        // And: page 1 size 1
        page = PageRequest.of(1, 1);
        Page<ApplicationListEntrySummaryProjection> page1 =
                applicationListEntryRepository.findSummariesById(
                        data1.getApplicationList().getUuid(), page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getNumberOfElements()).isEqualTo(1);
        assertThat(page1.getNumberOfElements()).isEqualTo(1);

        // Confirm ordering by sequence number asc
        assertThat(page0.getContent().getFirst().getSequenceNumber()).isEqualTo(sequenceNumber1);
        assertThat(page1.getContent().getFirst().getSequenceNumber()).isEqualTo(sequenceNumber2);
    }

    @Test
    public void testFindByIdForPrinting_returnsExpectedPrintProjection() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry data =
                ApplicationListEntryUtil.saveApplicationListEntry(
                        entityManager, persistance, list, (short) 1);

        // test get
        List<ApplicationListEntryPrintProjection>
                applicationListEntryPrintProjectionsToAssertAgainst =
                        applicationListEntryRepository.findByIdForPrinting(
                                data.getApplicationList().getUuid());

        // assert that the data that has been retrieved aligns with the data that we
        // have stored
        assertNotNull(applicationListEntryPrintProjectionsToAssertAgainst.getFirst());
        assertEquals(
                data.getId(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getId());
        assertEquals(
                data.getSequenceNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getSequenceNumber());
        assertEquals(
                data.getStandardApplicant().getApplicantTitle(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getApplicantTitle());
        assertEquals(
                data.getStandardApplicant().getApplicantSurname(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantSurname());
        assertEquals(
                data.getStandardApplicant().getApplicantForename1(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantForename1());
        assertEquals(
                data.getStandardApplicant().getApplicantForename2(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantForename2());
        assertEquals(
                data.getStandardApplicant().getApplicantForename3(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantForename3());
        assertEquals(
                data.getStandardApplicant().getAddressLine1(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantAddressLine1());
        assertEquals(
                data.getStandardApplicant().getAddressLine2(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantAddressLine2());
        assertEquals(
                data.getStandardApplicant().getAddressLine3(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantAddressLine3());
        assertEquals(
                data.getStandardApplicant().getAddressLine4(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantAddressLine4());
        assertEquals(
                data.getStandardApplicant().getAddressLine5(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantAddressLine5());
        assertEquals(
                data.getStandardApplicant().getPostcode(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantPostcode());
        assertEquals(
                data.getStandardApplicant().getTelephoneNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getApplicantPhone());
        assertEquals(
                data.getStandardApplicant().getMobileNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantMobile());
        assertEquals(
                data.getStandardApplicant().getEmailAddress(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getApplicantEmail());
        assertEquals(
                data.getStandardApplicant().getName(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getApplicantName());
        assertEquals(
                data.getRnameaddress().getTitle(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentTitle());
        assertEquals(
                data.getRnameaddress().getSurname(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentSurname());
        assertEquals(
                data.getRnameaddress().getForename1(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentForename1());
        assertEquals(
                data.getRnameaddress().getForename2(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentForename2());
        assertEquals(
                data.getRnameaddress().getForename3(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentForename3());
        assertEquals(
                data.getRnameaddress().getAddress1(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentAddressLine1());
        assertEquals(
                data.getRnameaddress().getAddress2(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentAddressLine2());
        assertEquals(
                data.getRnameaddress().getAddress3(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentAddressLine3());
        assertEquals(
                data.getRnameaddress().getAddress4(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentAddressLine4());
        assertEquals(
                data.getRnameaddress().getAddress5(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentAddressLine5());
        assertEquals(
                data.getRnameaddress().getPostcode(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentPostcode());
        assertEquals(
                data.getRnameaddress().getTelephoneNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentPhone());
        assertEquals(
                data.getRnameaddress().getMobileNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentMobile());
        assertEquals(
                data.getRnameaddress().getEmailAddress(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentEmail());
        assertEquals(
                data.getRnameaddress().getDateOfBirth(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentDateOfBirth());
        assertEquals(
                data.getRnameaddress().getName(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getRespondentName());
        assertEquals(
                data.getApplicationCode().getCode(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicationCode());
        assertEquals(
                data.getApplicationCode().getTitle(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicationTitle());
        assertEquals(
                data.getApplicationListEntryWording(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicationWording());
        assertEquals(
                data.getCaseReference(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getCaseReference());
        assertEquals(
                data.getAccountNumber(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getAccountReference());
        assertEquals(
                data.getNotes(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getNotes());
        assertThat(applicationListEntryPrintProjectionsToAssertAgainst.size()).isEqualTo(1);
    }

    @Test
    public void testGetListEntriesSearchWithNoSearchCriteria() {
        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "courtCode"));
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        false, null, null, null, null, null, null, null, null, null, null, null,
                        null, page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(10);
        assertThat(page0.getTotalPages()).isEqualTo(1);
        ApplicationListEntryGetSummaryProjection projection0 = page0.getContent().get(0);
        assertThat(projection0.getCjaCode()).isEqualTo("CJ");
        assertThat(projection0.getCourtCode()).isEqualTo("RCJ001");
        assertThat(projection0.getStatus()).isEqualTo(Status.OPEN);
        assertNotNull(projection0.getRnameAddress());
        assertThat(projection0.getTitle()).isEqualTo("Certificate of Satisfaction");
        assertNull(projection0.getAnameAddress());
        assertNotNull(projection0.getLegislation(), "");
        assertNotNull(projection0.getStandardApplicantCode(), "APP001");
        assertThat(projection0.getDateOfAl()).isEqualTo("2024-04-21");

        ApplicationListEntryGetSummaryProjection projection4 = page0.getContent().get(4);

        assertThat(projection4.getCjaCode()).isEqualTo("CJ");
        assertThat(projection4.getStatus()).isEqualTo(Status.OPEN);
        assertThat(projection4.getRnameAddress().getSurname()).isEqualTo("Johnson");
        assertThat(projection4.getRnameAddress().getName()).isEqualTo("Sarah Johnson");
        assertThat(projection4.getRnameAddress().getCode()).isEqualTo("RE");
        assertThat(projection4.getRnameAddress().getPostcode()).isEqualTo("XY9 8ZZ");
        assertThat(projection4.getFeeRequired()).isEqualTo(YesOrNo.YES);
        assertThat(projection4.getCourtCode()).isEqualTo("RCJ001");
        assertThat(projection4.getOtherLocationDescription()).isEqualTo("other");
        assertThat(projection4.getTitle()).isEqualTo("Copy documents");
        assertThat(projection4.getLegislation()).isEqualTo("");
        assertThat(projection4.getStandardApplicantCode()).isEqualTo("APP002");
    }

    @Test
    public void testGetListEntriesSearchForDataWithFullEntry() {
        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "courtCode"));
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        true,
                        LocalDate.parse("2024-04-21"),
                        "RCJ001",
                        "other",
                        "CJ",
                        null,
                        "Turner",
                        "APP002",
                        Status.OPEN,
                        "Sarah Johnson",
                        "Johnson",
                        "XY9 8ZZ",
                        "29345",
                        page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(1);
        assertThat(page0.getTotalPages()).isEqualTo(1);
        assertThat(page0.getContent().get(0).getCjaCode()).isEqualTo("CJ");
        assertThat(page0.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
        assertThat(page0.getContent().get(0).getAnameAddress().getSurname()).isEqualTo("Turner");
        assertThat(page0.getContent().get(0).getAnameAddress().getAddress1())
                .isEqualTo("1 Market Street");
        assertThat(page0.getContent().get(0).getAnameAddress().getEmailAddress())
                .isEqualTo("john.smith@example.com");
        assertThat(page0.getContent().get(0).getAnameAddress().getPostcode()).isEqualTo("AB11 2CD");
        assertThat(page0.getContent().get(0).getAnameAddress().getTelephoneNumber())
                .isEqualTo("01234567890");

        assertThat(page0.getContent().get(0).getRnameAddress().getSurname()).isEqualTo("Johnson");
        assertThat(page0.getContent().get(0).getRnameAddress().getName())
                .isEqualTo("Sarah Johnson");
        assertThat(page0.getContent().get(0).getRnameAddress().getCode()).isEqualTo("RE");
        assertThat(page0.getContent().get(0).getRnameAddress().getPostcode()).isEqualTo("XY9 8ZZ");
        assertThat(page0.getContent().get(0).getRnameAddress().getAddress1())
                .isEqualTo("12 The Avenue");
        assertThat(page0.getContent().get(0).getRnameAddress().getEmailAddress())
                .isEqualTo("s.johnson@example.com");

        assertThat(page0.getContent().get(0).getFeeRequired()).isEqualTo(YesOrNo.YES);
        assertThat(page0.getContent().get(0).getCourtCode()).isEqualTo("RCJ001");
        assertThat(page0.getContent().get(0).getOtherLocationDescription()).isEqualTo("other");
        assertThat(page0.getContent().get(0).getTitle()).isEqualTo("Copy documents");
        assertThat(page0.getContent().get(0).getLegislation()).isEqualTo("");
        assertThat(page0.getContent().get(0).getStandardApplicantCode()).isEqualTo("APP002");
    }

    @Test
    public void testGetListEntriesSearchForDataWithPartialsWherePossible() {
        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 20);
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        true,
                        LocalDate.parse("2025-04-21"),
                        "MCJC002",
                        null,
                        "CJ",
                        null,
                        null,
                        "PP001",
                        Status.OPEN,
                        "Jac",
                        "Turn",
                        "AB11 2CD",
                        null,
                        page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(1);
        assertThat(page0.getTotalPages()).isEqualTo(1);
        assertThat(page0.getContent().get(0).getCjaCode()).isEqualTo("CJ");
        assertThat(page0.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
        assertThat(page0.getContent().get(0).getFeeRequired()).isEqualTo(YesOrNo.NO);

        assertThat(page0.getContent().get(0).getRnameAddress().getAddress1())
                .isEqualTo("1 Market Street");
        assertThat(page0.getContent().get(0).getRnameAddress().getForename1()).isEqualTo("John");
        assertThat(page0.getContent().get(0).getRnameAddress().getSurname()).isEqualTo("Turner");

        assertThat(page0.getContent().get(0).getRnameAddress().getName()).isEqualTo("Jack Turner");
        assertThat(page0.getContent().get(0).getRnameAddress().getCode()).isEqualTo("RE");
        assertThat(page0.getContent().get(0).getRnameAddress().getPostcode()).isEqualTo("AB11 2CD");

        assertThat(page0.getContent().get(0).getTitle()).isEqualTo("Appeal by Case Stated (Crime)");
        assertThat(page0.getContent().get(0).getLegislation())
                .isEqualTo("Section 111 Magistrates' Courts Act 1980");
        assertThat(page0.getContent().get(0).getStandardApplicantCode()).isEqualTo("APP001");
    }

    @Test
    @Transactional
    public void testBulkMoveByUuidAndSourceList_movesOnlyMatchingEntriesAndReturnsCount() {
        // Given: source, target and other lists
        ApplicationList sourceList = new AppListTestData().someMinimal().build();
        persistance.save(sourceList);

        ApplicationList targetList = new AppListTestData().someMinimal().build();
        persistance.save(targetList);

        ApplicationList otherList = new AppListTestData().someMinimal().build();
        persistance.save(otherList);

        // Create entries:
        // - two entries in the source list that we expect to be moved
        // - one entry in the source list that is NOT in the uuid set (should not move)
        // - one entry in a different list that is included in the uuid set but must NOT move
        UUID moveUuid1 = saveEntryInSourceList(sourceList).getUuid();
        UUID moveUuid2 = saveEntryInSourceList(sourceList).getUuid();
        saveEntryInSourceList(sourceList);
        UUID wrongListUuid = saveEntryInSourceList(otherList).getUuid();

        // When: call the repository bulk-move with a set that includes moveUuid1, moveUuid2 and
        // wrongListUuid
        Set<UUID> uuidsToMove = Set.of(moveUuid1, moveUuid2, wrongListUuid);

        entityManager.flush();
        entityManager.clear();

        int updatedCount =
                applicationListEntryRepository.bulkMoveByUuidAndSourceList(
                        uuidsToMove, targetList, sourceList.getUuid());

        // Then: only the two entries in the source list are moved, and the method returns 2
        assertEquals(
                2,
                updatedCount,
                "Should report two rows updated (only entries in source list moved)");
    }

    private ApplicationListEntry saveEntryInSourceList(ApplicationList sourceList) {
        ApplicationListEntry moveEntry1 = new AppListEntryTestData().someMinimal().build();
        moveEntry1.setApplicationList(sourceList);
        persistance.save(moveEntry1);
        entityManager.refresh(moveEntry1);

        return moveEntry1;
    }
}
