package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;
import uk.gov.hmcts.appregister.data.AppListEntryResolutionTestData;
import uk.gov.hmcts.appregister.data.AppListEntryTestData;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.data.ResolutionCodeTestData;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
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
        ApplicationListEntry data = saveApplicationListEntry(list, (short) 1);

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
        ApplicationListEntry data1 = saveApplicationListEntry(list, sequenceNumber1);
        ApplicationListEntry data2 = saveApplicationListEntry(list, sequenceNumber2);

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
        ApplicationListEntry data = saveApplicationListEntry(list, (short) 1);

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
                data.getUuid(),
                applicationListEntryPrintProjectionsToAssertAgainst.getFirst().getUuid());
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
                data.getRnameaddress().getDateOfBirth().truncatedTo(ChronoUnit.MICROS),
                applicationListEntryPrintProjectionsToAssertAgainst
                        .getFirst()
                        .getRespondentDateOfBirth()
                        .truncatedTo(ChronoUnit.MICROS));
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

    private ApplicationListEntry saveApplicationListEntry(
            ApplicationList list, Short sequenceNumber) {
        ResolutionCode resolutionCode = new ResolutionCodeTestData().someComplete();
        entityManager.persist(resolutionCode);
        entityManager.flush();

        ApplicationListEntry listEntryData =
                new AppListEntryTestData().createApplicationListEntry(list, sequenceNumber);

        listEntryData.setAccountNumber("1234567890");
        StandardApplicant standardApplicant = new StandardApplicantTestData().someComplete();
        listEntryData.setStandardApplicant(standardApplicant);
        NameAddress nameAddress = new NameAddressTestData().someComplete();
        listEntryData.setRnameaddress(nameAddress);

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
        return data;
    }
}
