package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.data.FeeTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.DatabaseReset;
import uk.gov.hmcts.appregister.util.DateUtil;

@Slf4j
class FeeRepositoryTest extends BaseRepositoryTest {

    private final FeeRepository applicationFeeRepository;

    @Test
    void referenceExistenceIncludesExpiredMainFeesButNotOffsiteFees() {
        String reference = "ARCPOC1907";
        Fee expired = new FeeTestData().someMinimal().build();
        expired.setReference(reference);
        expired.setOffsite(false);
        expired.setStartDate(LocalDate.of(2024, 1, 1));
        expired.setEndDate(LocalDate.of(2024, 4, 30));
        persistance.save(expired);
        assertThat(
                        applicationFeeRepository.existsByReferenceIgnoreCaseAndIsOffsiteFalse(
                                "arcpoc1907"))
                .isTrue();
        assertThat(
                        applicationFeeRepository.findByReferenceBetweenDate(
                                reference, LocalDate.of(2026, 10, 22)))
                .isEmpty();
        assertThat(
                        applicationFeeRepository.findByReferenceBetweenDate(
                                reference, expired.getStartDate()))
                .hasSize(1);
        assertThat(
                        applicationFeeRepository.findByReferenceBetweenDate(
                                reference, expired.getEndDate()))
                .hasSize(1);
        assertThat(
                        applicationFeeRepository.existsByReferenceIgnoreCaseAndIsOffsiteFalse(
                                "UNKNOWN1907"))
                .isFalse();
        expired.setOffsite(true);
        applicationFeeRepository.saveAndFlush(expired);
        assertThat(applicationFeeRepository.existsByReferenceIgnoreCaseAndIsOffsiteFalse(reference))
                .isFalse();
    }

    @Autowired
    FeeRepositoryTest(FeeRepository applicationFeeRepository) {
        this.applicationFeeRepository = applicationFeeRepository;
    }

    @Test
    void effectivePeriodsSupportBackdatingWithoutRevivingSupersededFees() {
        var date = LocalDate.of(2026, 6, 9);
        for (var offsite : List.of(false, true)) {
            var reference = offsite ? "HISTOFF1878" : "HIST1878";
            var old =
                    new FeeTestData()
                            .someMinimal()
                            .id(DatabaseReset.SEQUENCE_START_VALUE + (offsite ? 187810L : 187800L))
                            .reference(reference)
                            .isOffsite(offsite)
                            .startDate(date.minusDays(10))
                            .endDate(null)
                            .build();
            var winner =
                    new FeeTestData()
                            .someMinimal()
                            .id(old.getId() + 1)
                            .reference(reference.toLowerCase())
                            .isOffsite(offsite)
                            .startDate(date.minusDays(10))
                            .endDate(date.plusDays(1))
                            .build();
            var future =
                    new FeeTestData()
                            .someMinimal()
                            .id(old.getId() + 2)
                            .reference(reference)
                            .isOffsite(offsite)
                            .startDate(date.plusDays(3))
                            .endDate(null)
                            .build();
            var expired =
                    new FeeTestData()
                            .someMinimal()
                            .id(old.getId() + 3)
                            .reference(reference)
                            .isOffsite(offsite)
                            .startDate(date.minusDays(100))
                            .endDate(date.minusDays(20))
                            .build();
            for (var fee : List.of(old, winner, future, expired)) {
                fee.setChangedBy(0L);
                fee.setChangedDate(OffsetDateTime.now());
                fee.setCreatedUser("test");
                persistance.save(fee);
            }
            for (var active : List.of(date.minusDays(10), date, date.plusDays(1))) {
                assertThat(
                                applicationFeeRepository
                                        .findByReferenceBetweenDateWithOffsite(
                                                reference, active, offsite)
                                        .getFirst()
                                        .getId())
                        .isEqualTo(winner.getId());
                if (offsite) {
                    assertThat(applicationFeeRepository.findOffsite(active).getFirst().getId())
                            .isEqualTo(winner.getId());
                } else {
                    assertThat(
                                    applicationFeeRepository
                                            .findByReferenceBetweenDate(reference, active)
                                            .getFirst()
                                            .getId())
                            .isEqualTo(winner.getId());
                    assertThat(
                                    applicationFeeRepository
                                            .findByReferenceInBetweenDate(
                                                    List.of(reference.toLowerCase()), active)
                                            .getFirst()
                                            .getId())
                            .isEqualTo(winner.getId());
                }
            }
            assertThat(
                            applicationFeeRepository
                                    .findByReferenceBetweenDateWithOffsite(
                                            reference, date.plusDays(3), offsite)
                                    .getFirst()
                                    .getId())
                    .isEqualTo(future.getId());
            assertThat(
                            applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                                    reference, date.minusDays(11), offsite))
                    .isEmpty();
            assertThat(
                            applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                                    reference, date.minusDays(20), offsite))
                    .extracting(Fee::getId)
                    .containsExactly(expired.getId());
            var replacement =
                    new FeeTestData()
                            .someMinimal()
                            .id(old.getId() + 4)
                            .reference(reference)
                            .isOffsite(offsite)
                            .startDate(winner.getStartDate())
                            .endDate(date.minusDays(1))
                            .build();
            replacement.setChangedBy(0L);
            replacement.setChangedDate(OffsetDateTime.now());
            replacement.setCreatedUser("test");
            persistance.save(replacement);
            for (var excluded : List.of(date, date.plusDays(2))) {
                assertThat(
                                applicationFeeRepository.findByReferenceBetweenDateWithOffsite(
                                        reference, excluded, offsite))
                        .isEmpty();
                assertThat(applicationFeeRepository.findByReferenceBetweenDate(reference, excluded))
                        .isEmpty();
                assertThat(
                                applicationFeeRepository.findByReferenceInBetweenDate(
                                        List.of(reference.toLowerCase()), excluded))
                        .isEmpty();
                assertThat(applicationFeeRepository.findOffsite(excluded))
                        .noneMatch(row -> row.getReference().equalsIgnoreCase(reference));
            }
            // Explicit stored IDs still identify their original versions, not the preferred
            // version.
            assertThat(
                            applicationFeeRepository
                                    .findByIdsBetweenDate(List.of(old.getId()), date)
                                    .getFirst()
                                    .getId())
                    .isEqualTo(old.getId());
        }
    }

    @Test
    void testBasicInsertionUpdate() {
        // assert that the save has occurred
        long count = applicationFeeRepository.count();

        // test save
        Fee fee = persistance.save(new FeeTestData().someComplete());
        assertEquals(count + 1, applicationFeeRepository.count());

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
    void testSearchForFeeWithoutOffsitePrefersHighestId() {
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

        assertEquals(1, results.size());
        assertEquals(Math.max(preferred.getId(), bounded.getId()), results.getFirst().getId());
    }

    @Test
    void testBatchSearchForFeeWithoutOffsiteGroupsByReferenceAndPrefersHighestId() {
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

        assertEquals(2, results.size());
        assertEquals(
                Math.max(firstPreferred.getId(), firstBounded.getId()), results.get(0).getId());
        assertEquals(secondFee.getId(), results.get(1).getId());
    }
}
