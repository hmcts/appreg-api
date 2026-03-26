package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.NationalCourtHouseData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

public class NationalCourtHouseRepositoryTest extends BaseRepositoryTest {

    @Autowired private NationalCourtHouseRepository repository;

    // --- findActiveCourt -----------------------------------------------------

    @Nested
    @DisplayName("findActiveCourt(code, date)")
    class FindActiveCourt {

        @Test
        @DisplayName(
                "returns seeded Cardiff (CCC003) when active on given date (case-insensitive code)")
        void returnsCardiff() {
            List<NationalCourtHouse> result =
                    repository.findActiveCourtsWithDate("ccc003", LocalDate.of(2025, 1, 1));

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
                                        .isBeforeOrEqualTo(LocalDate.of(2025, 1, 1));
                            });
        }

        @Test
        @DisplayName("returns seeded Bristol (BCC006) when active on given date")
        void returnsBristol() {
            List<NationalCourtHouse> result =
                    repository.findActiveCourtsWithDate("BCC006", LocalDate.of(2025, 1, 1));

            assertThat(result)
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactly("BCC006");
        }

        @Test
        @DisplayName("prefers the most recent null-end row when multiple courts share a code")
        void prefersMostRecentNullEndDateRow() {
            LocalDate activeDate = LocalDate.now();
            String code = "NCH-NULL-FIRST";

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
                    .containsExactly(savedNewerCourt.getId(), savedOlderCourt.getId());
        }

        @Test
        @DisplayName("returns bounded active rows and excludes future-dated open-ended rows")
        void returnsBoundedActiveRowsWithinDateWindow() {
            LocalDate activeDate = LocalDate.now();
            String code = "NCH-DATE-WINDOW";

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
                            null, null, LocalDate.now(), PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactlyInAnyOrder("CCC003", "BCC006");

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
                            "cc", null, LocalDate.now(), PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactlyInAnyOrder("CCC003", "BCC006");
        }

        @Test
        @DisplayName("filters by name contains (case-insensitive) — e.g. 'crown'")
        void filtersByName() {
            var page =
                    repository.findAllActiveCourts(
                            null, "crown", LocalDate.now(), PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getName)
                    .containsExactlyInAnyOrder("Cardiff Crown Court", "Bristol Crown Court");
        }

        @Test
        @DisplayName("filters by both code and name together")
        void filtersByCodeAndName() {
            var page =
                    repository.findAllActiveCourts(
                            "cc", "bristol", LocalDate.now(), PageRequest.of(0, 20));

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
                            null, null, LocalDate.now(), PageRequest.of(0, 1));
            var page2 =
                    repository.findAllActiveCourts(
                            null, null, LocalDate.now(), PageRequest.of(1, 1));

            assertThat(page1.getTotalElements()).isEqualTo(2);
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page2.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("findActiveCourts(code) excludes future rows and keeps bounded current rows")
        void activeCourtsUsesCurrentDateWindow() {
            LocalDate today = LocalDate.now();
            String code = "NCH-CURRENT-WINDOW";

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
            LocalDate today = LocalDate.now();
            String codePrefix = "NCH-ACTIVE-WINDOW";

            NationalCourtHouse boundedCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "-BOUNDED")
                            .name("Bounded Search Court")
                            .startDate(today.minusDays(3))
                            .endDate(today.plusDays(3))
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse futureCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "-FUTURE")
                            .name("Future Search Court")
                            .startDate(today.plusDays(1))
                            .endDate(null)
                            .courtType("CHOA")
                            .build();

            NationalCourtHouse expiredCourt =
                    new NationalCourtHouseData()
                            .someMinimal()
                            .courtLocationCode(codePrefix + "-EXPIRED")
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
                    .containsExactly(codePrefix + "-BOUNDED");
        }
    }
}
