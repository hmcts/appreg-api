package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

class ResolutionCodeRepositoryTest extends BaseRepositoryTest {

    @Autowired private ResolutionCodeRepository repository;


    @Nested
    @DisplayName("findActiveResolutionCodesOnDateByCode")
    class FindByCodeOnDate {

        @Test
        @DisplayName("returns seeded APPC when active on date (case-insensitive code)")
        void returnsAppc() {
            List<ResolutionCode> result =
                    repository.findActiveResolutionCodesOnDateByCode(
                            "appc", todayStart, tomorrowStart);

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(
                            rc -> {
                                assertThat(rc.getResultCode()).isEqualTo("APPC");
                                assertThat(rc.getTitle()).isEqualTo("Appeal to Crown Court");
                                assertThat(rc.getEndDate()).isNull(); // still active
                                assertThat(rc.getStartDate()).isBeforeOrEqualTo(todayStart);
                            });
        }
    }

    @Nested
    @DisplayName("findActiveOnDate (filters + paging)")
    class FindActiveOnDate {

        @Test
        @DisplayName("no filters -> seeded active rows include APPC/AUTH/CASE/COL")
        void noFilters() {
            var page =
                    repository.findActiveOnDate(
                            null, null, todayStart, tomorrowStart, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .contains("APPC", "AUTH", "CASE", "COL");
        }

        @Test
        @DisplayName("filters by code contains (case-insensitive) — e.g. 'ap'")
        void filtersByCode() {
            var page =
                    repository.findActiveOnDate(
                            "ap", null, todayStart, tomorrowStart, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("APPC");
        }

        @Test
        @DisplayName("filters by title contains (case-insensitive) — e.g. 'author'")
        void filtersByTitle() {
            var page =
                    repository.findActiveOnDate(
                            null, "author", todayStart, tomorrowStart, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("AUTH");
        }

        @Test
        @DisplayName("filters by both code and title together — e.g. code 'ca', title 'case'")
        void filtersByBoth() {
            var page =
                    repository.findActiveOnDate(
                            "ca", "case", todayStart, tomorrowStart, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("CASE");
        }
    }
}
