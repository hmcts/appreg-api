package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

public class StandardApplicantRepositoryTest extends BaseRepositoryTest {

    @Autowired private StandardApplicantRepository repository;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    // Count for the baseline integration test data in the flyway script.
    private static final int BASELINE_TEST_COUNT = 7;

    @Test
    public void testBasicInsertionUpdate() throws Exception {

        transactionalUnitOfWork.inTransaction(
                () -> {
                    // assert that the save has occurred
                    long count = repository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT, count);

                    // assert
                    // test save
                    StandardApplicant dataToPersist =
                            new StandardApplicantTestData().someComplete();
                    StandardApplicant data = persistance.save(dataToPersist);

                    // assert that the save has occurred
                    count = repository.count();
                    Assertions.assertEquals(BASELINE_TEST_COUNT + 1, count);

                    // test get
                    Optional<StandardApplicant> standardApplicantToAssertAgainst =
                            repository.findById(data.getId());

                    // assert that the data that has been retrieved aligns with the data that we
                    // have stored
                    assertFalse(standardApplicantToAssertAgainst.isEmpty());
                    expectAllCommonEntityFields(data, standardApplicantToAssertAgainst.get());
                    expectAllCommonEntityFields(
                            dataToPersist, standardApplicantToAssertAgainst.get());
                    assertEquals(
                            dataToPersist.getAddressLine1(),
                            standardApplicantToAssertAgainst.get().getAddressLine1());
                    assertEquals(
                            dataToPersist.getApplicantCode(),
                            standardApplicantToAssertAgainst.get().getApplicantCode());
                    assertEquals(
                            dataToPersist.getAddressLine2(),
                            standardApplicantToAssertAgainst.get().getAddressLine2());
                    assertEquals(
                            dataToPersist.getAddressLine3(),
                            standardApplicantToAssertAgainst.get().getAddressLine3());
                    assertEquals(
                            dataToPersist.getAddressLine4(),
                            standardApplicantToAssertAgainst.get().getAddressLine4());
                    assertEquals(
                            dataToPersist.getAddressLine5(),
                            standardApplicantToAssertAgainst.get().getAddressLine5());
                    assertEquals(
                            dataToPersist.getApplicantForename1(),
                            standardApplicantToAssertAgainst.get().getApplicantForename1());
                    assertEquals(
                            dataToPersist.getApplicantForename2(),
                            standardApplicantToAssertAgainst.get().getApplicantForename2());
                    assertEquals(
                            dataToPersist.getApplicantForename3(),
                            standardApplicantToAssertAgainst.get().getApplicantForename3());
                    assertEquals(
                            dataToPersist.getApplicantStartDate(),
                            standardApplicantToAssertAgainst.get().getApplicantStartDate());
                    assertEquals(
                            dataToPersist.getApplicantTitle(),
                            standardApplicantToAssertAgainst.get().getApplicantTitle());
                    assertEquals(
                            dataToPersist.getMobileNumber(),
                            standardApplicantToAssertAgainst.get().getMobileNumber());
                    assertEquals(
                            dataToPersist.getPostcode(),
                            standardApplicantToAssertAgainst.get().getPostcode());
                    assertEquals(
                            dataToPersist.getTelephoneNumber(),
                            standardApplicantToAssertAgainst.get().getTelephoneNumber());
                    assertEquals(
                            dataToPersist.getApplicantSurname(),
                            standardApplicantToAssertAgainst.get().getApplicantSurname());
                    assertEquals(
                            dataToPersist.getName(),
                            standardApplicantToAssertAgainst.get().getName());
                    assertEquals(
                            dataToPersist.getApplicantEndDate(),
                            standardApplicantToAssertAgainst.get().getApplicantEndDate());
                    assertEquals(
                            dataToPersist.getEmailAddress(),
                            standardApplicantToAssertAgainst.get().getEmailAddress());
                    assertEquals(
                            dataToPersist.getId(), standardApplicantToAssertAgainst.get().getId());
                });
    }

    @Test
    public void testFindByCodeAndDate() throws Exception {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    List<StandardApplicant> retrievedApplicant =
                            repository.findStandardApplicantByCodeAndDate(
                                    "APP002", LocalDate.now());

                    assertFalse(retrievedApplicant.isEmpty());
                });
    }

    @Test
    public void testFindByCodeAndDateMultiple() throws Exception {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    List<StandardApplicant> retrievedApplicant =
                            repository.findStandardApplicantByCodeAndDate(
                                    "APP003", LocalDate.now());

                    assertEquals(2, retrievedApplicant.size());
                });
    }

    @Test
    public void testFindByCodeAndDatePrefersNullEndDate() throws Exception {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    LocalDate activeDate = LocalDate.now();
                    String code = "SANULL001";

                    StandardApplicant boundedApplicant =
                            new StandardApplicantTestData().someComplete();
                    boundedApplicant.setApplicantCode(code);
                    boundedApplicant.setName("Bounded Applicant");
                    boundedApplicant.setApplicantStartDate(activeDate.minusDays(1));
                    boundedApplicant.setApplicantEndDate(activeDate.plusDays(7));

                    StandardApplicant openEndedApplicant =
                            new StandardApplicantTestData().someComplete();
                    openEndedApplicant.setApplicantCode(code);
                    openEndedApplicant.setName("Open-Ended Applicant");
                    openEndedApplicant.setApplicantStartDate(activeDate.minusDays(1));
                    openEndedApplicant.setApplicantEndDate(null);

                    StandardApplicant savedBoundedApplicant = persistance.save(boundedApplicant);
                    StandardApplicant savedOpenEndedApplicant =
                            persistance.save(openEndedApplicant);

                    List<StandardApplicant> retrievedApplicant =
                            repository.findStandardApplicantByCodeAndDate(code, activeDate);

                    assertThat(retrievedApplicant)
                            .extracting(StandardApplicant::getId)
                            .containsExactly(
                                    savedOpenEndedApplicant.getId(), savedBoundedApplicant.getId());
                });
    }

    @Test
    public void testSearchIncludesApplicantsStartingOrEndingOnActiveDate() throws Exception {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    LocalDate activeDate = LocalDate.now();
                    String code = "SABOUND001";

                    StandardApplicant startsTodayApplicant =
                            new StandardApplicantTestData().someComplete();
                    startsTodayApplicant.setApplicantCode(code);
                    startsTodayApplicant.setName("Starts Today Applicant");
                    startsTodayApplicant.setApplicantStartDate(activeDate);
                    startsTodayApplicant.setApplicantEndDate(null);

                    StandardApplicant endsTodayApplicant =
                            new StandardApplicantTestData().someComplete();
                    endsTodayApplicant.setApplicantCode(code);
                    endsTodayApplicant.setName("Ends Today Applicant");
                    endsTodayApplicant.setApplicantStartDate(activeDate.minusDays(5));
                    endsTodayApplicant.setApplicantEndDate(activeDate);

                    StandardApplicant savedStartsTodayApplicant =
                            persistance.save(startsTodayApplicant);
                    StandardApplicant savedEndsTodayApplicant =
                            persistance.save(endsTodayApplicant);

                    var page =
                            repository.search(
                                    code,
                                    null,
                                    activeDate,
                                    org.springframework.data.domain.PageRequest.of(0, 10));

                    assertThat(page.getContent())
                            .extracting(StandardApplicant::getId)
                            .containsExactlyInAnyOrder(
                                    savedStartsTodayApplicant.getId(),
                                    savedEndsTodayApplicant.getId());
                });
    }
}
