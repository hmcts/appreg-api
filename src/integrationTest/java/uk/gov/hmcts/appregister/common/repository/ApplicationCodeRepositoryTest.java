package uk.gov.hmcts.appregister.common.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.security.UserProvider;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;
import uk.gov.hmcts.appregister.testutils.DateUtil;
import uk.gov.hmcts.appregister.testutils.data.ApplicationCodeTestData;

@Slf4j
public class ApplicationCodeRepositoryTest extends BasePostgresIntegrationTest {

    @Autowired private ApplicationCodeRepository applicationCodeRepository;

    @Autowired private UserProvider loggedInUser;

    private static final int BASELINE_TEST_COUNT = 41;

    @Test
    public void testBasicInsertionUpdate() throws Exception {
        // assert that the save has occurred
        long count = applicationCodeRepository.count();
        Assertions.assertEquals(BASELINE_TEST_COUNT, count);

        // test save
        ApplicationCode code =
                persistance.save(new ApplicationCodeTestData().someMinimal().build());

        // assert that the save has occurred
        count = applicationCodeRepository.count();
        Assertions.assertEquals(BASELINE_TEST_COUNT + 1, count);

        // test get
        Optional<ApplicationCode> applicationCodeToAssertAgainst =
                applicationCodeRepository.findById(code.getId());

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(code, applicationCodeToAssertAgainst);
        assertNotNull(applicationCodeToAssertAgainst.get());
        assertEquals(code.getCreatedUser(), applicationCodeToAssertAgainst.get().getCreatedUser());
        assertEquals(
                code.getApplicationCode(),
                applicationCodeToAssertAgainst.get().getApplicationCode());
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
        assertEquals(loggedInUser.getUser(), applicationCodeToAssertAgainst.get().getCreatedUser());
        assertEquals(
                new BigDecimal(loggedInUser.getUserNumber()),
                applicationCodeToAssertAgainst.get().getChangedBy());
        assertNotNull(applicationCodeToAssertAgainst.get().getChangedDate());
        assertEquals(0, applicationCodeToAssertAgainst.get().getVersion());
        assertNull(
                code.getDestinationEmail1(),
                applicationCodeToAssertAgainst.get().getDestinationEmail1());
        assertNull(
                code.getDestinationEmail2(),
                applicationCodeToAssertAgainst.get().getDestinationEmail2());
    }
}
