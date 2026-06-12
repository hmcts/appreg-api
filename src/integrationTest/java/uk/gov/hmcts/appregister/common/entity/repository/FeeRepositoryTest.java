package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
class FeeRepositoryTest extends BaseRepositoryTest {

    private final FeeRepository applicationFeeRepository;

    @Autowired
    FeeRepositoryTest(FeeRepository applicationFeeRepository) {
        this.applicationFeeRepository = applicationFeeRepository;
    }

    private static final int BASELINE_TEST_COUNT = 23;

    @Test
    void testBasicInsertionUpdate() {
        // assert that the save has occurred
        long count = applicationFeeRepository.count();
        assertEquals(BASELINE_TEST_COUNT, count);

        // test save
        Fee fee = persistance.save(new FeeTestData().someComplete());

        // test get
        Optional<Fee> feeToAssertAgainst = applicationFeeRepository.findById(fee.getId());
        assertTrue(feeToAssertAgainst.isPresent());
        Fee savedFee = feeToAssertAgainst.orElseThrow();

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(fee, savedFee);
        assertNotNull(savedFee);
        assertEquals(fee.getAmount(), savedFee.getAmount());
        assertEquals(fee.getReference(), savedFee.getReference());
        assertEquals(fee.getDescription(), savedFee.getDescription());
        assertTrue(DateUtil.equalsIgnoreMillis(fee.getStartDate(), savedFee.getStartDate()));
    }

    @Test
    void testSearchForFeeWithoutOffsite() {
        assertNotNull(
                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                        "CO1.1", LocalDate.now(java.time.ZoneOffset.UTC), false));
    }

    @Test
    void testSearchForFeeWithOffsite() {
        assertNotNull(
                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                        "CO1.1", LocalDate.now(java.time.ZoneOffset.UTC), true));
    }

    @Test
    void testSearchForFeeWithoutOffsitePrefersNullEndDate() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
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

        assertEquals(2, results.size());
        assertEquals(preferred.getId(), results.getFirst().getId());
        assertNull(results.getFirst().getEndDate());
        assertEquals(bounded.getId(), results.get(1).getId());
    }

    @Test
    void testBatchSearchForFeeWithoutOffsiteGroupsByReferenceAndPrefersNullEndDate() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        String firstReference = "ZZFEE2";

        Fee firstBounded = new FeeTestData().someComplete();
        firstBounded.setReference(firstReference);
        firstBounded.setDescription("Bounded first fee");
        firstBounded.setAmount(BigDecimal.valueOf(12));
        firstBounded.setOffsite(false);
        firstBounded.setStartDate(today.minusDays(10));
        firstBounded.setEndDate(today.plusDays(10));
        firstBounded = persistance.save(firstBounded);

        Fee firstPreferred = new FeeTestData().someComplete();
        firstPreferred.setReference(firstReference);
        firstPreferred.setDescription("Open-ended first fee");
        firstPreferred.setAmount(BigDecimal.valueOf(34));
        firstPreferred.setOffsite(false);
        firstPreferred.setStartDate(today.minusDays(10));
        firstPreferred.setEndDate(null);
        firstPreferred = persistance.save(firstPreferred);

        String secondReference = "ZZFEE3";
        Fee secondFee = new FeeTestData().someComplete();
        secondFee.setReference(secondReference);
        secondFee.setDescription("Second fee");
        secondFee.setAmount(BigDecimal.valueOf(56));
        secondFee.setOffsite(false);
        secondFee.setStartDate(today.minusDays(10));
        secondFee.setEndDate(null);
        secondFee = persistance.save(secondFee);

        List<Fee> results =
                applicationFeeRepository.findByReferenceInBetweenDate(
                        List.of(firstReference.toLowerCase(), secondReference.toLowerCase()),
                        today);

        assertEquals(3, results.size());
        assertEquals(firstPreferred.getId(), results.get(0).getId());
        assertEquals(firstBounded.getId(), results.get(1).getId());
        assertEquals(secondFee.getId(), results.get(2).getId());
    }
}
