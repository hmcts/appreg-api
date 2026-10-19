package uk.gov.hmcts.appregister.common.entity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

class ResolutionCodeRepositoryTest extends BaseRepositoryTest {

    @Autowired private ResolutionCodeRepository repository;
    @Autowired private Clock clock; // from AppConfig (UTC) – keeps tests stable

    // Convenience helper: today as a LocalDate based on the injected Clock
    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of("Europe/London")));
    }

    @Nested
    @DisplayName("findActiveResolutionCodesOnDateByCode")
    class FindByCodeOnDate {

        @Test
        @DisplayName("returns seeded APPC when active on date (case-insensitive code)")
        void returnsAppc() {
            var result = repository.findActiveResolutionCodesByCodeAndDate("appc", today());

            assertThat(result)
                    .hasValueSatisfying(
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
            var page = repository.findActiveOnDate("AP", null, today(), PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("APPC", "APPABANDON");
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

        @Test
        @DisplayName("includes rows whose start date is exactly the active date")
        void includesRowStartingOnActiveDate() {
            LocalDate activeDate = today();
            ResolutionCode startsToday = new ResolutionCode();
            startsToday.setId(Math.abs(UUID.randomUUID().getMostSignificantBits()));
            startsToday.setResultCode("BOUNDARY1");
            startsToday.setTitle("Boundary Start");
            startsToday.setWording("Boundary wording");
            startsToday.setStartDate(activeDate);
            startsToday.setEndDate(null);
            startsToday.setChangedBy(1L);
            startsToday.setChangedDate(OffsetDateTime.now(clock));

            repository.saveAndFlush(startsToday);

            var page =
                    repository.findActiveOnDate(
                            "BOUNDARY1", null, activeDate, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(ResolutionCode::getResultCode)
                    .containsExactly("BOUNDARY1");
        }
    }

    @Nested
    @DisplayName("latest date-valid version")
    class LatestDateValidVersion {

        @Test
        void returnsSeededAppc() {
            var result = repository.findActiveResolutionCodesByCodeAndDate("appc", today());
            assertThat(result)
                    .hasValueSatisfying(
                            rc -> {
                                assertThat(rc.getResultCode()).isEqualTo("APPC");
                                assertThat(rc.getTitle()).isEqualTo("Appeal to Crown Court");
                                assertThat(rc.getEndDate()).isNull();
                            });
        }

        @Test
        void selectsHighestDateValidIdBeforeTitleFilteringAndPagination() {
            LocalDate t = today();

            ResolutionCode additionalActiveWithEndDate = new ResolutionCode();
            additionalActiveWithEndDate.setId(900000000L);
            additionalActiveWithEndDate.setResultCode("APPC");
            additionalActiveWithEndDate.setTitle("Latest bounded APPC");
            additionalActiveWithEndDate.setWording("Dummy wording");
            additionalActiveWithEndDate.setStartDate(t.minusDays(10));
            additionalActiveWithEndDate.setEndDate(t);
            additionalActiveWithEndDate.setChangedBy(1L);
            additionalActiveWithEndDate.setChangedDate(OffsetDateTime.now(clock));

            final var savedWithEndDate = repository.saveAndFlush(additionalActiveWithEndDate);

            var future = new ResolutionCode();
            future.setId(900000001L);
            future.setResultCode("APPC");
            future.setTitle("Future");
            future.setWording("Future wording");
            future.setStartDate(t.plusDays(1));
            future.setChangedBy(1L);
            future.setChangedDate(OffsetDateTime.now(clock));
            repository.saveAndFlush(future);
            var expired = new ResolutionCode();
            expired.setId(900000002L);
            expired.setResultCode("APPC");
            expired.setTitle("Expired");
            expired.setWording("Expired wording");
            expired.setStartDate(t.minusDays(10));
            expired.setEndDate(t.minusDays(1));
            expired.setChangedBy(1L);
            expired.setChangedDate(OffsetDateTime.now(clock));
            repository.saveAndFlush(expired);
            assertThat(repository.findActiveResolutionCodesByCodeAndDate("appc", t))
                    .map(ResolutionCode::getId)
                    .contains(savedWithEndDate.getId());
            assertThat(repository.findActiveResolutionCodesByCodeAndDate("appc", t.minusDays(10)))
                    .map(ResolutionCode::getId)
                    .contains(expired.getId());
            var page = repository.findActiveOnDate("APPC", null, t, PageRequest.of(0, 1));
            assertThat(page.getContent())
                    .extracting(ResolutionCode::getId)
                    .containsExactly(savedWithEndDate.getId());
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(
                            repository.findActiveOnDate(
                                    "APPC", "Appeal to Crown Court", t, PageRequest.of(0, 10)))
                    .isEmpty();
            var first =
                    repository.findActiveOnDate(
                            "AP", null, t, PageRequest.of(0, 1, Sort.by("resultCode")));
            var second =
                    repository.findActiveOnDate(
                            "AP", null, t, PageRequest.of(1, 1, Sort.by("resultCode")));
            assertThat(first.getTotalElements()).isEqualTo(2);
            assertThat(second.getTotalElements()).isEqualTo(2);
            assertThat(first.getContent().getFirst().getResultCode())
                    .isNotEqualTo(second.getContent().getFirst().getResultCode());
        }

        @Test
        @DisplayName("returns empty when no active row exists for the given code")
        void returnsEmpty_whenNotFound() {
            var result =
                    repository.findActiveResolutionCodesByCodeAndDate("DOES_NOT_EXIST", today());

            assertThat(result).isEmpty();
        }
    }
}
