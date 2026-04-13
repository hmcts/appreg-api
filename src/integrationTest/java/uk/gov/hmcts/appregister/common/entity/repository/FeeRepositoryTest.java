package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
public class FeeRepositoryTest extends BaseRepositoryTest {

    @Autowired private FeeRepository applicationFeeRepository;

    private static final int BASELINE_TEST_COUNT = 23;

    @Test
    public void testBasicInsertionUpdate() throws Exception {
        // assert that the save has occurred
        long count = applicationFeeRepository.count();
        Assertions.assertEquals(BASELINE_TEST_COUNT, count);

        // test save
        Fee fee = persistance.save(new FeeTestData().someComplete());

        // test get
        Optional<Fee> feeToAssertAgainst = applicationFeeRepository.findById(fee.getId());

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(fee, feeToAssertAgainst.get());
        assertNotNull(feeToAssertAgainst.get());
        assertEquals(fee.getAmount(), feeToAssertAgainst.get().getAmount());
        assertEquals(fee.getReference(), feeToAssertAgainst.get().getReference());
        assertEquals(fee.getDescription(), feeToAssertAgainst.get().getDescription());
        assertTrue(
                DateUtil.equalsIgnoreMillis(
                        fee.getStartDate(), feeToAssertAgainst.get().getStartDate()));
    }
}
