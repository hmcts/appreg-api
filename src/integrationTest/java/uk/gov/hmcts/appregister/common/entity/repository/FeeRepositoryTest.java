package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    public void testSearchForFeeWithoutOffsite() {
        Assertions.assertNotNull(
                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                        "CO1.1", LocalDate.now(), false));
    }

    @Test
    public void testSearchForFeeWithOffsite() {
        Assertions.assertNotNull(
                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                        "CO1.1", LocalDate.now(), true));
    }

    @Test
    public void testSearchForFeeWithoutOffsitePrefersNullEndDate() {
        LocalDate today = LocalDate.now();
        String reference = "ZZFEE1";

        Fee bounded = new FeeTestData().someComplete();
        bounded.setReference(reference);
        bounded.setDescription("Bounded overlap fee");
        bounded.setAmount(BigDecimal.valueOf(12));
        bounded.setOffsite(false);
        bounded.setStartDate(today.minusDays(10));
        bounded.setEndDate(today.plusDays(10));
        bounded = persistance.save(bounded);

        Fee preferred = new FeeTestData().someComplete();
        preferred.setReference(reference);
        preferred.setDescription("Open-ended overlap fee");
        preferred.setAmount(BigDecimal.valueOf(34));
        preferred.setOffsite(false);
        preferred.setStartDate(today.minusDays(10));
        preferred.setEndDate(null);
        preferred = persistance.save(preferred);

        List<Fee> results =
                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                        reference, today, false);

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals(preferred.getId(), results.getFirst().getId());
        Assertions.assertNull(results.getFirst().getEndDate());
        Assertions.assertEquals(bounded.getId(), results.get(1).getId());
    }
}
