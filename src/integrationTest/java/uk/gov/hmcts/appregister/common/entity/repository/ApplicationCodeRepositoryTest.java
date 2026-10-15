package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
class ApplicationCodeRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private UserProvider loggedInUser;

    // The total app codes inserted by flyway scripts. See V6__InitialTestData.sql
    private static final int TOTAL_APP_CODES_COUNT = 223;

    @Test
    void testBasicInsertionUpdate() {
        // assert that the save has occurred
        long count = applicationCodeRepository.count();
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT, count);

        ApplicationCode codeToSave = new ApplicationCodeTestData().someComplete();

        // test save
        ApplicationCode code = persistance.save(codeToSave);

        // assert that the save has occurred
        count = applicationCodeRepository.count();
        Assertions.assertEquals(TOTAL_APP_CODES_COUNT + 1L, count);

        // test get
        Optional<ApplicationCode> applicationCodeToAssertAgainst =
                applicationCodeRepository.findById(code.getId());

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(code, applicationCodeToAssertAgainst.get());
        assertNotNull(applicationCodeToAssertAgainst.get());
        assertEquals(code.getCreatedUser(), applicationCodeToAssertAgainst.get().getCreatedUser());
        assertEquals(code.getCode(), applicationCodeToAssertAgainst.get().getCode());
        assertEquals(code.getCreatedUser(), applicationCodeToAssertAgainst.get().getCreatedUser());
        assertTrue(
                DateUtil.equalsIgnoreMillis(
                        code.getEndDate(), applicationCodeToAssertAgainst.get().getEndDate()));
        assertTrue(
                DateUtil.equalsIgnoreMillis(
                        code.getStartDate(), applicationCodeToAssertAgainst.get().getStartDate()));
        assertEquals(
                code.getBulkRespondentAllowed(),
                applicationCodeToAssertAgainst.get().getBulkRespondentAllowed());
        assertEquals(code.getCreatedUser(), applicationCodeToAssertAgainst.get().getCreatedUser());
        assertEquals(code.getChangedBy(), applicationCodeToAssertAgainst.get().getChangedBy());
        assertNotNull(applicationCodeToAssertAgainst.get().getChangedDate());
        assertEquals(0, applicationCodeToAssertAgainst.get().getVersion());
        assertEquals(
                code.getDestinationEmail1(),
                applicationCodeToAssertAgainst.get().getDestinationEmail1());
        assertEquals(
                code.getDestinationEmail2(),
                applicationCodeToAssertAgainst.get().getDestinationEmail2());
    }

    @Test
    void testGetByCodeAndDate() {
        List<ApplicationCode> applicationCodeToAssertAgainst =
                applicationCodeRepository.findByCodeAndDate(
                        "AD99002", LocalDate.now(java.time.ZoneOffset.UTC));
        assertThat(applicationCodeToAssertAgainst).isNotEmpty();
    }

    @Test
    void testGetByCodeAndDatePrefersNullEndDate() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        String code = "ZZ90011";

        ApplicationCode bounded = new ApplicationCodeTestData().someComplete();
        bounded.setCode(code);
        bounded.setTitle("Bounded overlap");
        bounded.setStartDate(today.minusDays(10));
        bounded.setEndDate(today.plusDays(10));
        bounded = persistance.save(bounded);

        ApplicationCode preferred = new ApplicationCodeTestData().someComplete();
        preferred.setCode(code);
        preferred.setTitle("Open-ended overlap");
        preferred.setStartDate(today.minusDays(10));
        preferred.setEndDate(null);
        preferred = persistance.save(preferred);

        List<ApplicationCode> results = applicationCodeRepository.findByCodeAndDate(code, today);

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals(preferred.getId(), results.getFirst().getId());
        Assertions.assertNull(results.getFirst().getEndDate());
        Assertions.assertEquals(bounded.getId(), results.get(1).getId());
    }

    @Test
    void testGetByCodeAndDateUsesExactCodeMatch() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        String code = "ZZ90012";

        ApplicationCode exactMatch = new ApplicationCodeTestData().someComplete();
        exactMatch.setCode(code);
        exactMatch.setTitle("Exact code");
        exactMatch.setStartDate(today.minusDays(10));
        exactMatch.setEndDate(today.plusDays(10));
        exactMatch = persistance.save(exactMatch);

        ApplicationCode partialMatch = new ApplicationCodeTestData().someComplete();
        partialMatch.setCode("P" + code + "Q");
        partialMatch.setTitle("Partial code");
        partialMatch.setStartDate(today.minusDays(10));
        partialMatch.setEndDate(null);
        persistance.save(partialMatch);

        List<ApplicationCode> results = applicationCodeRepository.findByCodeAndDate(code, today);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(exactMatch.getId(), results.getFirst().getId());
        Assertions.assertEquals(code, results.getFirst().getCode());
    }

    @Test
    void testSearchExcludesCodeBeforeStartDate() {
        LocalDate effectiveDate = LocalDate.of(2020, Month.JANUARY, 1);
        ApplicationCode applicationCode =
                saveApplicationCode(
                        "ZZ90101", "Starts after search date", effectiveDate.plusDays(1), null);

        var results =
                applicationCodeRepository.search(
                        applicationCode.getCode(), null, effectiveDate, PageRequest.of(0, 10));

        Assertions.assertEquals(0, results.getTotalElements());
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void testSearchIncludesCodeStartingOnActiveDate() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);

        ApplicationCode startsToday = saveApplicationCode("ZZ90013", "Starts Today", today, null);

        var results =
                applicationCodeRepository.search(
                        startsToday.getCode(), null, today, PageRequest.of(0, 10));

        Assertions.assertEquals(1, results.getTotalElements());
        Assertions.assertEquals(startsToday.getId(), results.getContent().getFirst().getId());
    }

    @Test
    void testSearchIncludesCodeBetweenStartAndEndDate() {
        LocalDate effectiveDate = LocalDate.of(2020, Month.JANUARY, 15);
        ApplicationCode applicationCode =
                saveApplicationCode(
                        "ZZ90102",
                        "Effective between dates",
                        effectiveDate.minusDays(10),
                        effectiveDate.plusDays(10));

        var results =
                applicationCodeRepository.search(
                        applicationCode.getCode(), null, effectiveDate, PageRequest.of(0, 10));

        Assertions.assertEquals(1, results.getTotalElements());
        Assertions.assertEquals(applicationCode.getId(), results.getContent().getFirst().getId());
    }

    @Test
    void testSearchExcludesCodeAfterEndDate() {
        LocalDate effectiveDate = LocalDate.of(2020, Month.JANUARY, 15);
        ApplicationCode applicationCode =
                saveApplicationCode(
                        "ZZ90103",
                        "Ended before search date",
                        effectiveDate.minusDays(10),
                        effectiveDate.minusDays(1));

        var results =
                applicationCodeRepository.search(
                        applicationCode.getCode(), null, effectiveDate, PageRequest.of(0, 10));

        Assertions.assertEquals(0, results.getTotalElements());
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void testSearchIncludesCodeWithNullEndDate() {
        LocalDate effectiveDate = LocalDate.of(2020, Month.JANUARY, 15);
        ApplicationCode applicationCode =
                saveApplicationCode(
                        "ZZ90104", "Open ended effective code", effectiveDate.minusDays(10), null);

        var results =
                applicationCodeRepository.search(
                        applicationCode.getCode(), null, effectiveDate, PageRequest.of(0, 10));

        Assertions.assertEquals(1, results.getTotalElements());
        Assertions.assertEquals(applicationCode.getId(), results.getContent().getFirst().getId());
    }

    @Test
    void testSearchPaginationCountsAreBasedOnDateFilteredResults() {
        LocalDate effectiveDate = LocalDate.of(2020, Month.JANUARY, 15);
        String code = "ZZ90105";

        saveApplicationCode(
                code,
                "Date filtered alpha",
                effectiveDate.minusDays(10),
                effectiveDate.plusDays(10));
        final ApplicationCode sortedFirst =
                saveApplicationCode(
                        code,
                        "Date filtered zebra",
                        effectiveDate.minusDays(10),
                        effectiveDate.plusDays(10));
        saveApplicationCode(code, "Date filtered future", effectiveDate.plusDays(1), null);

        var results =
                applicationCodeRepository.search(
                        code,
                        null,
                        effectiveDate,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "title")));

        Assertions.assertEquals(1, results.getContent().size());
        Assertions.assertEquals(sortedFirst.getId(), results.getContent().getFirst().getId());
        Assertions.assertEquals(2, results.getTotalElements());
        Assertions.assertEquals(2, results.getTotalPages());
    }

    private ApplicationCode saveApplicationCode(
            String code, String title, LocalDate startDate, LocalDate endDate) {
        ApplicationCode applicationCode = new ApplicationCodeTestData().someComplete();
        applicationCode.setCode(code);
        applicationCode.setTitle(title);
        applicationCode.setStartDate(startDate);
        applicationCode.setEndDate(endDate);
        return persistance.save(applicationCode);
    }
}
