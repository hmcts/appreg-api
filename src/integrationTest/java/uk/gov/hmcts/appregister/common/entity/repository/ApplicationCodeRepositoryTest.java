package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.ApplicationCodeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
public class ApplicationCodeRepositoryTest extends BaseRepositoryTest {

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private UserProvider loggedInUser;

    // The total app codes inserted by flyway scripts. See V6__InitialTestData.sql
    private static final int TOTAL_APP_CODES_COUNT = 45;

    @Test
    public void testBasicInsertionUpdate() {
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
    public void testGetByCodeAndDate() {
        List<ApplicationCode> applicationCodeToAssertAgainst =
                applicationCodeRepository.findByCodeAndDate("AD99002", LocalDate.now());
        Assertions.assertFalse(applicationCodeToAssertAgainst.isEmpty());
    }

    @Test
    public void testGetByCodeAndDatePrefersNullEndDate() {
        LocalDate today = LocalDate.now();
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
    public void testGetByCodeAndDateUsesExactCodeMatch() {
        LocalDate today = LocalDate.now();
        String code = "ZZ90012";

        ApplicationCode exactMatch = new ApplicationCodeTestData().someComplete();
        exactMatch.setCode(code);
        exactMatch.setTitle("Exact code");
        exactMatch.setStartDate(today.minusDays(10));
        exactMatch.setEndDate(today.plusDays(10));
        exactMatch = persistance.save(exactMatch);

        ApplicationCode partialMatch = new ApplicationCodeTestData().someComplete();
        partialMatch.setCode("PRE" + code + "POST");
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
    public void testSearchIncludesCodeStartingOnActiveDate() {
        LocalDate today = LocalDate.now();
        String code = "ZZ90013";

        ApplicationCode startsToday = new ApplicationCodeTestData().someComplete();
        startsToday.setCode(code);
        startsToday.setTitle("Starts Today");
        startsToday.setStartDate(today);
        startsToday.setEndDate(null);
        startsToday = persistance.save(startsToday);

        var results =
                applicationCodeRepository.search(
                        code, null, today, org.springframework.data.domain.PageRequest.of(0, 10));

        Assertions.assertEquals(1, results.getTotalElements());
        Assertions.assertEquals(startsToday.getId(), results.getContent().getFirst().getId());
    }
}
