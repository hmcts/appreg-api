package uk.gov.hmcts.appregister.report.service;

import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.report.normaliser.ReportFilterNormaliser;

class ReportFilterNormaliserTest extends AbstractAsyncTest {
    private final ReportFilterNormaliser normaliser = new ReportFilterNormaliser();

    @Test
    void givenActivityAuditDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));

        ActivityAuditFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, 5, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, 5, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenFeesDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));

        FeesReportFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, 5, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, 5, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenDurationDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1));

        DurationFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, 5, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, 5, 31), normalisedFilter.getDateTo());
    }
}
