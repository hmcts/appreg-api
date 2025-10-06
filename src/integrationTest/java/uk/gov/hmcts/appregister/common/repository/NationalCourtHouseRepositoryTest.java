package uk.gov.hmcts.appregister.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
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
    }

    // --- findAllActiveCourts -------------------------------------------------

    @Nested
    @DisplayName("findAllActiveCourts(code, name, pageable)")
    class FindAllActiveCourts {

        @Test
        @DisplayName("no filters -> only active CHOA rows from seed (CCC003, BCC006)")
        void onlyActiveChoaRows() {
            var page = repository.findAllActiveCourts(null, null, PageRequest.of(0, 20));

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
            var page = repository.findAllActiveCourts("cc", null, PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactlyInAnyOrder("CCC003", "BCC006");
        }

        @Test
        @DisplayName("filters by name contains (case-insensitive) — e.g. 'crown'")
        void filtersByName() {
            var page = repository.findAllActiveCourts(null, "crown", PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getName)
                    .containsExactlyInAnyOrder("Cardiff Crown Court", "Bristol Crown Court");
        }

        @Test
        @DisplayName("filters by both code and name together")
        void filtersByCodeAndName() {
            var page = repository.findAllActiveCourts("cc", "bristol", PageRequest.of(0, 20));

            assertThat(page.getContent())
                    .extracting(NationalCourtHouse::getCourtLocationCode)
                    .containsExactly("BCC006");
        }

        @Test
        @DisplayName("supports pagination over seeded rows")
        void supportsPagination() {
            // We have exactly two active CHOA rows in the seed.
            var page1 = repository.findAllActiveCourts(null, null, PageRequest.of(0, 1));
            var page2 = repository.findAllActiveCourts(null, null, PageRequest.of(1, 1));

            assertThat(page1.getTotalElements()).isEqualTo(2);
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page2.getContent()).hasSize(1);
        }
    }
}
