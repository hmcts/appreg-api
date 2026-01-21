package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    @Autowired private Clock clock; // from AppConfig (UTC) – keeps tests stable

    // Convenience helper: today as a LocalDate based on the injected Clock
    private LocalDate today() {
        return LocalDate.now(clock);
    }

    @Nested
    @DisplayName("findActiveResolutionCodesOnDateByCode")
    class FindByCodeOnDate {

        @Test
        @DisplayName("returns seeded APPC when active on date (case-insensitive code)")
        void returnsAppc() {
            List<ResolutionCode> result =
                    repository.findActiveResolutionCodesByCodeAndDate("appc", today());

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(
                            rc -> {
                                assertThat(rc.getResultCode()).isEqualTo("APPC");
                                assertThat(rc.getTitle()).isEqualTo("Appeal to Crown Court");
                                assertThat(rc.getEndDate()).isNull();
                            });
        }
    }

    @Nested
    @DisplayName("findActiveOnDate (filters + paging)")
    class FindActiveOnDate {

        @Test
        @DisplayName("no filters -> seeded active rows include APPC/AUTH/CASE/COL")
        void noFilters() {
            var page = repository.findActiveOnDate(null, null, today(), PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .contains("APPC", "AUTH", "CASE", "COL");
        }

        @Test
        @DisplayName("filters by code contains (case-insensitive) — e.g. 'ap'")
        void filtersByCode() {
            var page = repository.findActiveOnDate("ap", null, today(), PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("APPC");
        }

        @Test
        @DisplayName("filters by title contains (case-insensitive) — e.g. 'author'")
        void filtersByTitle() {
            var page = repository.findActiveOnDate(null, "author", today(), PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("AUTH");
        }

        @Test
        @DisplayName("filters by both code and title together — e.g. code 'ca', title 'case'")
        void filtersByBoth() {
            var page = repository.findActiveOnDate("ca", "case", today(), PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("CASE");
        }
    }

    @Nested
    @DisplayName("findActiveByResultCodeIgnoreCasePreferNullEndDate")
    class FindActiveByResultCodePreferNullEndDate {

        @Test
        @DisplayName("returns seeded APPC when active (case-insensitive code) and endDate is null")
        void returnsSeededAppc_prefersNullEndDate() {
            List<ResolutionCode> result =
                    repository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                            "appc", PageRequest.of(0, 1));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getResultCode()).isEqualTo("APPC");
            assertThat(result.getFirst().getTitle()).isEqualTo("Appeal to Crown Court");
            assertThat(result.getFirst().getEndDate()).isNull();
        }

        @Test
        @DisplayName(
                "when multiple rows are active for the same code, prefers endDate = null over a future endDate")
        void prefersNullEndDate_whenMultipleActive() {
            LocalDate t = today();

            ResolutionCode additionalActiveWithEndDate = new ResolutionCode();
            additionalActiveWithEndDate.setResultCode("APPC");
            additionalActiveWithEndDate.setTitle("Some other APPC title (should not win)");
            additionalActiveWithEndDate.setWording("Dummy wording");
            additionalActiveWithEndDate.setStartDate(t.minusDays(10));
            additionalActiveWithEndDate.setEndDate(t.plusDays(10));
            additionalActiveWithEndDate.setChangedBy(1L);
            additionalActiveWithEndDate.setChangedDate(OffsetDateTime.now(clock));

            repository.saveAndFlush(additionalActiveWithEndDate);

            List<ResolutionCode> result =
                    repository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                            "APPC", PageRequest.of(0, 1));

            assertThat(result).hasSize(1);

            // Key behavioural assertion: open-ended row should be chosen
            assertThat(result.getFirst().getEndDate()).isNull();

            assertThat(result.getFirst().getTitle()).isEqualTo("Appeal to Crown Court");
        }

        @Test
        @DisplayName("returns empty when no active row exists for the given code")
        void returnsEmpty_whenNotFound() {
            List<ResolutionCode> result =
                    repository.findActiveByResultCodeIgnoreCasePreferNullEndDate(
                            "DOES_NOT_EXIST", PageRequest.of(0, 1));

            assertThat(result).isEmpty();
        }
    }
}
