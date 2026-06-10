package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.appregister.testutils.util.ApplicationListEntryUtil.saveApplicationListEntry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.base.EntryCount;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryGetSummaryProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResolutionProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.util.DateUtil;

@Transactional
@Rollback
class AppListEntryRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationListEntryRepository applicationListEntryRepository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void testBasicInsertionUpdate() {
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
    void testFindSummariesById_returnsExpectedSummaryProjection() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry data =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);

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
                        .getStandardApplicant()
                        .getName());
        assertEquals(
                data.getRnameaddress().getName(),
                applicationListEntrySummaryProjectionsToAssertAgainst
                        .getContent()
                        .getFirst()
                        .getRespondent()
                        .getName());
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
    void testFindSummariesById_paging_and_sorting() {
        // Given
        ApplicationList list = new AppListTestData().someMinimal().build();
        Short sequenceNumber1 = (short) 1;
        Short sequenceNumber2 = (short) 2;
        ApplicationListEntry data1 =
                saveApplicationListEntry(entityManager, persistance, list, sequenceNumber1);
        saveApplicationListEntry(entityManager, persistance, list, sequenceNumber2);

        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 1, Sort.by("sequenceNumber").ascending());
        Page<ApplicationListEntrySummaryProjection> page0 =
                applicationListEntryRepository.findSummariesById(
                        data1.getApplicationList().getUuid(), page);

        // And: page 1 size 1
        page = PageRequest.of(1, 1, Sort.by("sequenceNumber").ascending());
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
    void testFindSummariesById_excludesDeletedEntries() {
        // Given: an application list with one entry
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        ApplicationListEntry entry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);

        entry.setDeleted(true);
        entityManager.flush();
        entityManager.clear();

        // When: invoking findSummariesById for the list
        Pageable page = PageRequest.of(0, 10);
        Page<ApplicationListEntrySummaryProjection> result =
                applicationListEntryRepository.findSummariesById(list.getUuid(), page);

        // Then: result should be empty because the entry is deleted
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent().isEmpty()).isTrue();
    }

    @Test
    void testFindByIdForPrinting_returnsExpectedPrintProjection() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        ApplicationListEntry data =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);

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
                        .getApplicantLastName());
        assertEquals(
                data.getStandardApplicant().getApplicantForename1(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantFirstName());
        assertEquals(
                uk.gov.hmcts.appregister.common.mapper.ApplicantMapper.combineMiddleName(
                        data.getStandardApplicant().getApplicantForename2(),
                        data.getStandardApplicant().getApplicantForename3()),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getApplicantMiddleName());
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
                data.getRnameaddress().getLastName(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentLastName());
        assertEquals(
                data.getRnameaddress().getFirstName(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentFirstName());
        assertEquals(
                data.getRnameaddress().getMiddleName(),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentMiddleName());
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
    void testFindByIdForPrinting_excludesDeletedApplicationListEntries() {
        // Given: an application list with one deleted entry
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        ApplicationListEntry savedEntry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);

        savedEntry.setDeleted(true);

        entityManager.flush();
        entityManager.clear();

        // When
        List<ApplicationListEntryPrintProjection> result =
                applicationListEntryRepository.findByIdForPrinting(list.getUuid());

        // Then
        assertThat(result.isEmpty());
    }

    @Test
    void testGetListEntriesSearchWithNoSearchCriteria() {
        // When: page 0 size 1
        Pageable page =
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Direction.DESC, "courtCode")
                                .and(Sort.by(Sort.Direction.ASC, "id")));
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        null, false, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(11);
        assertThat(page0.getTotalPages()).isEqualTo(1);
        ApplicationListEntryGetSummaryProjection projection0 = page0.getContent().get(4);
        assertThat(projection0.getCjaCode()).isEqualTo("CJ");
        assertThat(projection0.getCourtCode()).isEqualTo("RCJ001");
        assertThat(projection0.getStatus()).isEqualTo(Status.OPEN);
        assertNotNull(projection0.getRnameAddress());
        assertThat(projection0.getTitle()).isEqualTo("Copy documents");
        assertNotNull(projection0.getAnameAddress());
        assertNotNull(projection0.getLegislation(), "");
        assertNotNull(projection0.getStandardApplicantCode(), "APP001");
        assertThat(projection0.getDateOfAl()).isEqualTo("2024-04-21");
    }

    @Test
    void testGetListEntriesSearchWithSearchCriteria() {
        // Searching without filter to get a list id to then filter.
        Pageable page =
                PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Direction.DESC, "courtCode")
                                .and(Sort.by(Sort.Direction.ASC, "id")));
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, page);

        ApplicationListEntryGetSummaryProjection projection0 = page0.getContent().get(4);

        // When: page 0 size 1
        Page<ApplicationListEntryGetSummaryProjection> page1 =
                applicationListEntryRepository.searchForGetSummary(
                        UUID.fromString(projection0.getListId()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        page);

        // Then
        assertThat(page1.getTotalElements()).isEqualTo(1);
        assertThat(page1.getTotalPages()).isEqualTo(1);
        ApplicationListEntryGetSummaryProjection projection1 = page1.getContent().getFirst();
        assertThat(projection1.getCjaCode()).isEqualTo("CJ");
        assertThat(projection1.getCourtCode()).isEqualTo("RCJ001");
        assertThat(projection1.getStatus()).isEqualTo(Status.OPEN);
        assertNotNull(projection1.getRnameAddress());
        assertThat(projection1.getTitle()).isEqualTo("Copy documents");
        assertNotNull(projection1.getAnameAddress());
        assertNotNull(projection1.getLegislation(), "");
        assertNotNull(projection1.getStandardApplicantCode(), "APP001");
        assertThat(projection1.getDateOfAl()).isEqualTo("2024-04-21");
    }

    @Test
    void testGetListEntriesSearchForDataWithFullEntry() {
        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "courtCode"));
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        null,
                        true,
                        LocalDate.parse("2024-04-21"),
                        "RCJ001",
                        "other",
                        "CJ",
                        null,
                        "Turner",
                        null,
                        "APP002",
                        Status.OPEN,
                        "Sarah Johnson",
                        null,
                        null,
                        "XY9 8ZZ",
                        "29345",
                        null,
                        null,
                        null,
                        null,
                        page);

        // Then
        assertThat(page0.getTotalElements()).isEqualTo(1);
        assertThat(page0.getTotalPages()).isEqualTo(1);
        assertThat(page0.getContent().get(0).getCjaCode()).isEqualTo("CJ");
        assertThat(page0.getContent().get(0).getStatus()).isEqualTo(Status.OPEN);
        assertThat(page0.getContent().get(0).getAnameAddress().getLastName()).isEqualTo("Turner");
        assertThat(page0.getContent().get(0).getAnameAddress().getAddress1())
                .isEqualTo("1 Market Street");
        assertThat(page0.getContent().get(0).getAnameAddress().getEmailAddress())
                .isEqualTo("john.smith@example.com");
        assertThat(page0.getContent().get(0).getAnameAddress().getPostcode()).isEqualTo("AB11 2CD");
        assertThat(page0.getContent().get(0).getAnameAddress().getTelephoneNumber())
                .isEqualTo("01234567890");

        assertThat(page0.getContent().get(0).getRnameAddress().getLastName()).isNull();
        assertThat(page0.getContent().get(0).getRnameAddress().getName())
                .isEqualTo("Sarah Johnson");
        assertThat(page0.getContent().get(0).getRnameAddress().getCode())
                .isEqualTo(NameAddressCodeType.RESPONDENT);
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
    void testGetListEntriesSearchForDataWithPartialsWherePossible() {
        // When: page 0 size 1
        Pageable page = PageRequest.of(0, 20);
        Page<ApplicationListEntryGetSummaryProjection> page0 =
                applicationListEntryRepository.searchForGetSummary(
                        null,
                        true,
                        LocalDate.parse("2025-04-21"),
                        "MCJC002",
                        null,
                        "CJ",
                        null,
                        null,
                        null,
                        "PP001",
                        Status.OPEN,
                        null,
                        "Turn",
                        "John",
                        "AB11 2CD",
                        null,
                        null,
                        null,
                        null,
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
        assertThat(page0.getContent().get(0).getRnameAddress().getFirstName()).isEqualTo("John");
        assertThat(page0.getContent().get(0).getRnameAddress().getLastName()).isEqualTo("Turner");

        assertThat(page0.getContent().get(0).getRnameAddress().getName()).isNull();
        assertThat(page0.getContent().get(0).getRnameAddress().getCode())
                .isEqualTo(NameAddressCodeType.RESPONDENT);
        assertThat(page0.getContent().get(0).getRnameAddress().getPostcode()).isEqualTo("AB11 2CD");

        assertThat(page0.getContent().get(0).getTitle()).isEqualTo("Appeal by Case Stated (Crime)");
        assertThat(page0.getContent().get(0).getLegislation())
                .isEqualTo("Section 111 Magistrates' Courts Act 1980");
        assertThat(page0.getContent().get(0).getStandardApplicantCode()).isEqualTo("APP001");
    }

    @Test
    void testSearchForGetSummary_excludesDeletedApplicationListEntries() {
        // Given: an application list with unique values to isolate this test
        ApplicationList list = new AppListTestData().someMinimal().build();
        list.setCourtCode("UNQ001");
        LocalDate hearingDate = LocalDate.of(2030, Month.JANUARY, 1);
        list.setDate(hearingDate);
        persistance.save(list);

        // And: two entries in the list
        saveApplicationListEntry(entityManager, persistance, list, (short) 1);

        ApplicationListEntry deletedEntry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 2);

        ApplicationListEntry managedDeletedEntry =
                entityManager.find(ApplicationListEntry.class, deletedEntry.getId());

        managedDeletedEntry.setDeleted(true);

        // Flush and clear so repository query reads from DB
        entityManager.flush();
        entityManager.clear();

        // When: calling searchForGetSummary
        Pageable page = PageRequest.of(0, 10);
        Page<ApplicationListEntryGetSummaryProjection> result =
                applicationListEntryRepository.searchForGetSummary(
                        null,
                        true,
                        hearingDate,
                        "UNQ001",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        page);

        // Then: only the non-deleted entry is returned
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testCountByApplicationListUuids_excludesDeletedEntries() {
        // Given: two lists
        ApplicationList listA = new AppListTestData().someMinimal().build();
        persistance.save(listA);

        ApplicationList listB = new AppListTestData().someMinimal().build();
        persistance.save(listB);

        // And: two entries for listA (we will delete one of them)
        saveApplicationListEntry(entityManager, persistance, listA, (short) 1);
        ApplicationListEntry deletedEntryA =
                saveApplicationListEntry(entityManager, persistance, listA, (short) 2);

        entityManager.flush();

        ApplicationListEntry managedDeletedEntryA =
                entityManager.find(ApplicationListEntry.class, deletedEntryA.getId());
        managedDeletedEntryA.setDeleted(true);

        entityManager.flush();

        // And: one entry for listB
        saveApplicationListEntry(entityManager, persistance, listB, (short) 1);

        // flush/clear so query reads from DB
        entityManager.flush();
        entityManager.clear();

        // When: invoking the repository method with the non-deleted lists' UUIDs
        List<EntryCount> counts =
                applicationListEntryRepository.countByApplicationListUuids(
                        List.of(listA.getUuid(), listB.getUuid()));

        // Then: both lists are returned, but listA count excludes the deleted entry
        assertThat(counts.size()).isEqualTo(2);

        Map<UUID, Long> countsByUuid =
                counts.stream()
                        .collect(Collectors.toMap(EntryCount::getPrimaryKey, EntryCount::getCount));

        // listA had 2 entries originally but one was marked deleted -> expect 1
        assertEquals(1L, countsByUuid.get(listA.getUuid()).longValue());

        // listB has 1 entry
        assertEquals(1L, countsByUuid.get(listB.getUuid()).longValue());
    }

    @Test
    void testFindByUuidAndApplicationListUuid_excludesDeletedEntries() {
        // Given: an application list and an entry
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        ApplicationListEntry savedEntry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);

        // Ensure the entry can be found when not deleted
        Optional<ApplicationListEntry> foundBeforeDelete =
                applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        savedEntry.getUuid(), list.getUuid());
        assertTrue(foundBeforeDelete.isPresent(), "Entry should be found before marking deleted");

        // Mark the entry as deleted (soft delete)
        ApplicationListEntry managed =
                entityManager.find(ApplicationListEntry.class, savedEntry.getId());
        managed.setDeleted(true);

        // Flush/clear so the repository query reads from DB
        entityManager.flush();
        entityManager.clear();

        // When: calling the repository method for the same UUIDs
        Optional<ApplicationListEntry> foundAfterDelete =
                applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        savedEntry.getUuid(), list.getUuid());

        // Then: the result should be empty because the entry is soft-deleted
        assertTrue(
                foundAfterDelete.isEmpty(), "Soft-deleted entries should be excluded from results");
    }

    @Test
    void testSearchForFilterByApplicantSurname() {
        // Given: an application list with entries that have distinct applicant surnames
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        ApplicationListEntry savedEntry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1, false);
        savedEntry.getAnamedaddress().setLastName("TargetApplicantSurname");

        ApplicationListEntry otherEntry =
                saveApplicationListEntry(entityManager, persistance, list, (short) 2, false);
        otherEntry.getAnamedaddress().setLastName("OtherApplicantSurname");

        entityManager.flush();
        entityManager.clear();

        // When: calling the repository method for the surname of the first applicant
        Page<ApplicationListEntryGetSummaryProjection> applicationListEntryList =
                applicationListEntryRepository.searchForGetSummary(
                        list.getUuid(),
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        savedEntry.getAnamedaddress().getLastName(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Pageable.ofSize(10));

        // Then: the entry added is the entry returned
        Assertions.assertThat(applicationListEntryList.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(savedEntry.getUuid().toString())
                .isEqualTo(applicationListEntryList.stream().findFirst().get().getUuid());
    }

    @Test
    @Transactional
    void testFindExistingEntryIdsInSourceList_returnsOnlyMatchingIds() {
        // Given: source and another list
        ApplicationList sourceList = new AppListTestData().someMinimal().build();
        persistance.save(sourceList);

        ApplicationList otherList = new AppListTestData().someMinimal().build();
        persistance.save(otherList);

        // And: two entries in the source list
        UUID sourceUuid1 = saveEntryInSourceList(sourceList).getUuid();
        UUID sourceUuid2 = saveEntryInSourceList(sourceList).getUuid();

        // And: one entry in a different list
        UUID otherUuid = saveEntryInSourceList(otherList).getUuid();

        // When: asking for existing IDs in the source list
        Set<UUID> requestedIds = Set.of(sourceUuid1, sourceUuid2, otherUuid);

        entityManager.flush();
        entityManager.clear();

        Set<UUID> result =
                applicationListEntryRepository.findExistingEntryIdsInSourceList(
                        sourceList.getUuid(), requestedIds);

        // Then: only the IDs that belong to the source list are returned
        assertEquals(Set.of(sourceUuid1, sourceUuid2), result);
        assertThat(result).doesNotContain(otherUuid);
    }

    @Test
    @Transactional
    void testFindResolutionCodesByEntryIds_returnsAllResolutionCodesForEachEntry() {
        ApplicationList list = new AppListTestData().someMinimal().build();
        persistance.save(list);

        ApplicationListEntry entry1 =
                saveApplicationListEntry(entityManager, persistance, list, (short) 1);
        ApplicationListEntry entry2 =
                saveApplicationListEntry(entityManager, persistance, list, (short) 2);

        saveResolution(entry1, "RC1");
        saveResolution(entry1, "RC2");
        saveResolution(entry2, "RC3");

        entityManager.flush();
        entityManager.clear();

        List<ApplicationListEntryResolutionProjection> result =
                applicationListEntryRepository.findResolutionCodesByEntryIds(
                        List.of(entry1.getId(), entry2.getId()));

        List<String> entry1Codes =
                result.stream()
                        .filter(p -> p.getEntryId().equals(entry1.getId()))
                        .map(p -> p.getResolutionCode().getResultCode())
                        .toList();

        List<String> entry2Codes =
                result.stream()
                        .filter(p -> p.getEntryId().equals(entry2.getId()))
                        .map(p -> p.getResolutionCode().getResultCode())
                        .toList();

        assertThat(entry1Codes).contains("RC1");
        assertThat(entry1Codes).contains("RC2");
        assertThat(entry2Codes).contains("RC3");
    }

    private ApplicationListEntry saveEntryInSourceList(ApplicationList sourceList) {
        ApplicationListEntry moveEntry1 = new AppListEntryTestData().someMinimal().build();
        moveEntry1.setApplicationList(sourceList);
        persistance.save(moveEntry1);
        entityManager.refresh(moveEntry1);

        return moveEntry1;
    }

    private void saveResolution(ApplicationListEntry entry, String resultCode) {
        ResolutionCode code = new ResolutionCode();
        code.setResultCode(resultCode);
        code.setTitle(resultCode + " title");
        code.setWording(resultCode + " wording");
        code.setLegislation("Test legislation");
        code.setStartDate(LocalDate.now(java.time.ZoneOffset.UTC));
        code.setChangedBy(1L);
        code.setChangedDate(OffsetDateTime.now(java.time.ZoneOffset.UTC));

        AppListEntryResolution entryResolution = new AppListEntryResolution();
        entryResolution.setApplicationList(entry);
        entryResolution.setResolutionCode(code);
        entryResolution.setResolutionWording(resultCode + " wording");
        entryResolution.setResolutionOfficer("Test officer");

        persistance.save(entryResolution);
    }
}
