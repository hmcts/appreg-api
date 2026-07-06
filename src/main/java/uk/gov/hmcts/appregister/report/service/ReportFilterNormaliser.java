package uk.gov.hmcts.appregister.report.service;

import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;

@Component
public class ReportFilterNormaliser {
    public ActivityAuditFilterDto normalise(ActivityAuditFilterDto filter) {
        return normalise(
                filter,
                ActivityAuditFilterDto::getDateFrom,
                ActivityAuditFilterDto::getDateTo,
                ActivityAuditFilterDto::setDateFrom,
                ActivityAuditFilterDto::setDateTo);
    }

    public FeesReportFilterDto normalise(FeesReportFilterDto filter) {
        return normalise(
                filter,
                FeesReportFilterDto::getDateFrom,
                FeesReportFilterDto::getDateTo,
                FeesReportFilterDto::setDateFrom,
                FeesReportFilterDto::setDateTo);
    }

    public SearchWarrantsReportFilterDto normalise(SearchWarrantsReportFilterDto filter) {
        return normalise(
                filter,
                SearchWarrantsReportFilterDto::getDateFrom,
                SearchWarrantsReportFilterDto::getDateTo,
                SearchWarrantsReportFilterDto::setDateFrom,
                SearchWarrantsReportFilterDto::setDateTo);
    }

    public DurationFilterDto normalise(DurationFilterDto filter) {
        return normalise(
                filter,
                DurationFilterDto::getDateFrom,
                DurationFilterDto::getDateTo,
                DurationFilterDto::setDateFrom,
                DurationFilterDto::setDateTo);
    }

    public ListMaintenanceFilterDto normalise(ListMaintenanceFilterDto filter) {
        return normalise(
                filter,
                ListMaintenanceFilterDto::getDateFrom,
                ListMaintenanceFilterDto::getDateTo,
                ListMaintenanceFilterDto::setDateFrom,
                ListMaintenanceFilterDto::setDateTo);
    }

    public PrivateProsecutorsIndexFilterDto normalise(PrivateProsecutorsIndexFilterDto filter) {
        return normalise(
                filter,
                PrivateProsecutorsIndexFilterDto::getDateFrom,
                PrivateProsecutorsIndexFilterDto::getDateTo,
                PrivateProsecutorsIndexFilterDto::setDateFrom,
                PrivateProsecutorsIndexFilterDto::setDateTo);
    }

    public WorkloadFilterDto normalise(WorkloadFilterDto filter) {
        return normalise(
                filter,
                WorkloadFilterDto::getDateFrom,
                WorkloadFilterDto::getDateTo,
                WorkloadFilterDto::setDateFrom,
                WorkloadFilterDto::setDateTo);
    }

    private <T> T normalise(
            T filter,
            Function<T, LocalDate> getDateFrom,
            Function<T, LocalDate> getDateTo,
            BiConsumer<T, LocalDate> setDateFrom,
            BiConsumer<T, LocalDate> setDateTo) {
        var dateFrom = getDateFrom.apply(filter);
        var dateTo = getDateTo.apply(filter);

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            setDateFrom.accept(filter, dateTo);
            setDateTo.accept(filter, dateFrom);
        }

        return filter;
    }
}
