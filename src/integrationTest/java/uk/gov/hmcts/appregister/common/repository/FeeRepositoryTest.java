package uk.gov.hmcts.appregister.common.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.repository.FeeRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.testutils.BasePostgresIntegrationTest;
import uk.gov.hmcts.appregister.testutils.DateUtil;
import uk.gov.hmcts.appregister.testutils.data.FeeTestData;

@Slf4j
public class FeeRepositoryTest extends BasePostgresIntegrationTest {

    @Autowired private FeeRepository applicationFeeRepository;

    @Autowired private UserProvider loggedInUser;

    private static final int BASELINE_TEST_COUNT = 23;

    @Test
    public void testBasicInsertionUpdate() throws Exception {
        // assert that the save has occurred
        long count = applicationFeeRepository.count();
        Assertions.assertEquals(BASELINE_TEST_COUNT, count);

        // test save
        Fee fee = persistance.save(new FeeTestData().someMinimal().build());

        // test get
        Optional<Fee> applicationCodeToAssertAgainst =
                applicationFeeRepository.findById(fee.getId());

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(fee, applicationFeeRepository);
        assertNotNull(applicationCodeToAssertAgainst.get());
        assertEquals(fee.getAmount(), applicationCodeToAssertAgainst.get().getAmount());
        assertEquals(fee.getReference(), applicationCodeToAssertAgainst.get().getReference());
        assertEquals(fee.getDescription(), applicationCodeToAssertAgainst.get().getDescription());
        assertTrue(
                DateUtil.equalsIgnoreMillis(
                        fee.getStartDate(), applicationCodeToAssertAgainst.get().getStartDate()));
    }
}
