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
}
