package uk.gov.hmcts.appregister.report.service;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;

class ReportFilterNormaliserTest {
    private final ReportFilterNormaliser normaliser = new ReportFilterNormaliser();

    @Test
    void givenActivityAuditDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        ActivityAuditFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenFeesDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        FeesReportFilterDto filter =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        FeesReportFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenDurationDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        DurationFilterDto filter =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        DurationFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenListMaintenanceDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        ListMaintenanceFilterDto filter =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        ListMaintenanceFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenSearchWarrantsDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        SearchWarrantsReportFilterDto filter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        SearchWarrantsReportFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenPrivateProsecutorsDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        PrivateProsecutorsIndexFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenWorkloadDateRangeIsReversed_whenNormalised_thenDatesAreSwapped() {
        WorkloadFilterDto filter =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1));

        WorkloadFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenActivityAuditDateFromIsNull_whenNormalised_thenFilterIsUnchanged() {
        ActivityAuditFilterDto filter =
                new ActivityAuditFilterDto().dateTo(LocalDate.of(2018, Month.MAY, 1));

        ActivityAuditFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertNull(normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateTo());
    }

    @Test
    void givenWorkloadDateToIsNull_whenNormalised_thenFilterIsUnchanged() {
        WorkloadFilterDto filter =
                new WorkloadFilterDto().dateFrom(LocalDate.of(2018, Month.MAY, 31));

        WorkloadFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateFrom());
        Assertions.assertNull(normalisedFilter.getDateTo());
    }

    @Test
    void givenSearchWarrantsDateRangeIsOrdered_whenNormalised_thenFilterIsUnchanged() {
        SearchWarrantsReportFilterDto filter =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31));

        SearchWarrantsReportFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 1), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }

    @Test
    void givenPrivateProsecutorsDateRangeIsEqual_whenNormalised_thenFilterIsUnchanged() {
        PrivateProsecutorsIndexFilterDto filter =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31));

        PrivateProsecutorsIndexFilterDto normalisedFilter = normaliser.normalise(filter);

        Assertions.assertSame(filter, normalisedFilter);
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateFrom());
        Assertions.assertEquals(LocalDate.of(2018, Month.MAY, 31), normalisedFilter.getDateTo());
    }
}
