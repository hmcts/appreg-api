package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;
import uk.gov.hmcts.appregister.testutils.DatabaseReset;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;

class NationalCourtHouseRepositoryTest extends BaseRepositoryTest {

    @Autowired private NationalCourtHouseRepository repository;
    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @Test
    void selectsEffectivePeriodBeforeEndDateTypeFilteringAndPagination() throws Exception {
        transactionalUnitOfWork.inTransaction(
                () -> {
                    var date = LocalDate.of(2026, 6, 9);
                    var old =
                            new NationalCourtHouseData()
                                    .someMinimal()
                                    .id(DatabaseReset.SEQUENCE_START_VALUE + 187801L)
                                    .courtLocationCode("HIST1878")
                                    .name("Original court")
                                    .courtType("CHOA")
                                    .startDate(date.minusDays(10))
                                    .endDate(null)
                                    .build();
                    var latest =
                            new NationalCourtHouseData()
                                    .someMinimal()
                                    .id(DatabaseReset.SEQUENCE_START_VALUE + 187802L)
                                    .courtLocationCode("hist1878")
                                    .name("Latest court")
                                    .courtType("CHOA")
                                    .startDate(date.minusDays(1))
                                    .endDate(date.plusDays(1))
                                    .build();
                    var future =
                            new NationalCourtHouseData()
                                    .someMinimal()
                                    .id(DatabaseReset.SEQUENCE_START_VALUE + 187803L)
                                    .courtLocationCode("HIST1878")
                                    .name("Future court")
                                    .courtType("CHOA")
                                    .startDate(date.plusDays(3))
                                    .endDate(null)
                                    .build();
                    var otherType =
                            new NationalCourtHouseData()
                                    .someMinimal()
                                    .id(DatabaseReset.SEQUENCE_START_VALUE + 187804L)
                                    .courtLocationCode("HIST1878")
                                    .name("Other type")
                                    .courtType("OTHER")
                                    .startDate(date.plusDays(4))
                                    .endDate(null)
                                    .build();
                    persistance.save(old);
                    persistance.save(latest);
                    persistance.save(future);
                    persistance.save(otherType);
                    for (var active : List.of(date.minusDays(1), date, date.plusDays(1))) {
                        assertThat(repository.findActiveCourtsWithDate("HIST1878", active))
                                .extracting(NationalCourtHouse::getId)
                                .containsExactly(latest.getId());
                        assertThat(repository.findActiveCourts("HIST1878", active))
                                .extracting(NationalCourtHouse::getId)
                                .containsExactly(latest.getId());
                        var result =
                                repository.findAllActiveCourts(
                                        "HIST1878", null, active, PageRequest.of(0, 1));
                        assertThat(result.getTotalElements()).isEqualTo(1);
                        assertThat(result.getContent())
                                .extracting(NationalCourtHouse::getId)
                                .containsExactly(latest.getId());
                        assertThat(
                                        repository.findAllActiveCourts(
                                                "HIST1878",
                                                "Original",
                                                active,
                                                PageRequest.of(0, 1)))
                                .isEmpty();
                        assertThat(
                                        repository.findAllActiveCourts(
                                                "HIST1878", null, active, PageRequest.of(1, 1)))
                                .isEmpty();
                    }
                    assertThat(repository.findActiveCourts("HIST1878", date.minusDays(2)))
                            .extracting(NationalCourtHouse::getId)
                            .containsExactly(old.getId());
                    assertThat(repository.findActiveCourts("HIST1878", date.plusDays(3)))
                            .extracting(NationalCourtHouse::getId)
                            .containsExactly(future.getId());
                    assertThat(repository.findActiveCourts("HIST1878", date.minusDays(11)))
                            .isEmpty();
                    var replacement =
                            new NationalCourtHouseData()
                                    .someMinimal()
                                    .id(DatabaseReset.SEQUENCE_START_VALUE + 187805L)
                                    .courtLocationCode("HIST1878")
                                    .name("Expired correction")
                                    .courtType("CHOA")
                                    .startDate(latest.getStartDate())
                                    .endDate(date.minusDays(1))
                                    .build();
                    persistance.save(replacement);
                    for (var excluded : List.of(date, date.plusDays(2), date.plusDays(4))) {
                        assertThat(repository.findActiveCourts("HIST1878", excluded)).isEmpty();
                        assertThat(repository.findActiveCourtsWithDate("HIST1878", excluded))
                                .isEmpty();
                        assertThat(
                                        repository
                                                .findAllActiveCourts(
                                                        "HIST1878",
                                                        null,
                                                        excluded,
                                                        PageRequest.of(0, 1))
                                                .getTotalElements())
                                .isZero();
                    }
                    assertThat(repository.findActiveCourts("HIST1878", date.minusDays(1)))
                            .extracting(NationalCourtHouse::getId)
                            .containsExactly(replacement.getId());
                    assertThat(repository.findActiveCourts("HIST1878", date.minusDays(2)))
                            .extracting(NationalCourtHouse::getId)
                            .containsExactly(old.getId());
                });
    }

    // --- findActiveCourt -----------------------------------------------------

    @Nested
    @DisplayName("findActiveCourt(code, date)")
    class FindActiveCourt {

        @Test
        @DisplayName(
                "returns seeded Cardiff (CCC003) when active on given date (case-insensitive code)")
        void returnsCardiff() {
            List<NationalCourtHouse> result =
                    repository.findActiveCourtsWithDate(
                            "ccc003", LocalDate.of(2025, Month.JANUARY, 1));

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(
                            n -> {
                                assertThat(n.getCourtType()).isEqualTo("CHOA");
                                assertThat(n.getEndDate()).isNull();
                                assertThat(n.getCourtLocationCode()).isEqualTo("CCC003");
                                assertThat(n.getName()).containsIgnoringCase("Cardiff");
                                assertThat(n.getStartDate())
                                        .isBeforeOrEqualTo(LocalDate.of(2025, Month.JANUARY, 1));
                            });
        }

        @Test
        @DisplayName("returns seeded Bristol (BCC006) when active on given date")
        void returnsBristol() {
            List<NationalCourtHouse> result =
                    repository.findActiveCourtsWithDate(
                            "BCC006", LocalDate.of(2025, Month.JANUARY, 1));

            assertThat(result)
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactly("BCC006");
        }

        @Test
        @DisplayName("selects the highest-ID date-valid row when multiple courts share a code")
        void selectsHighestId() {
            LocalDate activeDate = LocalDate.now(java.time.ZoneOffset.UTC);
            String code = "NCHNF001";

            NationalCourtHouse olderCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Older Court")
                            .startDate(activeDate.minusDays(10))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse newerCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Newer Court")
                            .startDate(activeDate.minusDays(1))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse savedOlderCourt = persistance.save(olderCourt);
            NationalCourtHouse savedNewerCourt = persistance.save(newerCourt);

            List<NationalCourtHouse> result = repository.findActiveCourtsWithDate(code, activeDate);

            assertThat(result)
                    .extracting(NationalCourtHouse::getId)
                    .containsExactly(Math.max(savedNewerCourt.getId(), savedOlderCourt.getId()));
        }

        @Test
        @DisplayName("returns bounded active rows and excludes future-dated open-ended rows")
        void returnsBoundedActiveRowsWithinDateWindow() {
            LocalDate activeDate = LocalDate.now(java.time.ZoneOffset.UTC);
            String code = "NCHDW001";

            NationalCourtHouse boundedCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Bounded Active Court")
                            .startDate(activeDate.minusDays(10))
                            .endDate(activeDate.plusDays(10))
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse futureCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Future Open Court")
                            .startDate(activeDate.plusDays(1))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse savedBoundedCourt = persistance.save(boundedCourt);
            persistance.save(futureCourt);

            List<NationalCourtHouse> result = repository.findActiveCourtsWithDate(code, activeDate);

            assertThat(result)
                    .extracting(NationalCourtHouse::getId)
                    .containsExactly(savedBoundedCourt.getId());
        }
    }

    // --- findAllActiveCourts -------------------------------------------------

    @Nested
    @DisplayName("findAllActiveCourts(code, name, pageable)")
    class FindAllActiveCourts {

        @Test
        @DisplayName("no filters -> only active CHOA rows from seed (CCC003, BCC006)")
        void onlyActiveChoaRows() {
            var page =
                    repository.findAllActiveCourts(
                            null,
                            null,
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .contains("CCC003", "BCC006");

            assertThat(page.getContent())
                    .allSatisfy(
                            n -> {
                                assertThat(n.getCourtType()).isEqualTo("CHOA");
                                assertThat(n.getEndDate()).isNull();
                            });
        }

        @Test
        @DisplayName("filters by code contains (case-insensitive) — e.g. 'cc'")
        void filtersByCode() {
            var page =
                    repository.findAllActiveCourts(
                            "cc",
                            null,
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactlyInAnyOrder("CCC003", "BCC006", "B13CC00", "C47CC00");
        }

        @Test
        @DisplayName("filters by name contains (case-insensitive) — e.g. 'crown'")
        void filtersByName() {
            var page =
                    repository.findAllActiveCourts(
                            null,
                            "crown",
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getName)
                    .contains("Cardiff Crown Court", "Bristol Crown Court");
        }

        @Test
        @DisplayName("filters by both code and name together")
        void filtersByCodeAndName() {
            var page =
                    repository.findAllActiveCourts(
                            "cc",
                            "bristol",
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactly("BCC006");
        }

        @Test
        @DisplayName("supports pagination over seeded rows")
        void supportsPagination() {
            // We have exactly two active CHOA rows in the seed.
            var page1 =
                    repository.findAllActiveCourts(
                            null,
                            null,
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(0, 1));
            var page2 =
                    repository.findAllActiveCourts(
                            null,
                            null,
                            LocalDate.now(java.time.ZoneOffset.UTC),
                            PageRequest.of(1, 1));

            assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(2);
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page2.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("findActiveCourts(code) excludes future rows and keeps bounded current rows")
        void activeCourtsUsesCurrentDateWindow() {
            LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
            String code = "NCHCW001";

            NationalCourtHouse boundedCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Bounded Current Court")
                            .startDate(today.minusDays(5))
                            .endDate(today.plusDays(5))
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse futureCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(code)
                            .name("Future Court")
                            .startDate(today.plusDays(2))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse savedBoundedCourt = persistance.save(boundedCourt);
            persistance.save(futureCourt);

            List<NationalCourtHouse> result = repository.findActiveCourts(code, today);

            assertThat(result)
                    .extracting(NationalCourtHouse::getId)
                    .containsExactly(savedBoundedCourt.getId());
        }

        @Test
        @DisplayName("findAllActiveCourts only returns rows active today")
        void allActiveCourtsFiltersToCurrentDateWindow() {
            LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
            String codePrefix = "NAW";

            NationalCourtHouse boundedCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "BND")
                            .name("Bounded Search Court")
                            .startDate(today.minusDays(3))
                            .endDate(today.plusDays(3))
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse futureCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "FUT")
                            .name("Future Search Court")
                            .startDate(today.plusDays(1))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse expiredCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "EXP")
                            .name("Expired Search Court")
                            .startDate(today.minusDays(10))
                            .endDate(today.minusDays(1))
                            .courtType("CHOA")
                            .build();

            persistance.save(boundedCourt);
            persistance.save(futureCourt);
            persistance.save(expiredCourt);

            var page =
                    repository.findAllActiveCourts(codePrefix, null, today, PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactly(codePrefix + "BND");
        }
    }
}
