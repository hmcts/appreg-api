package uk.gov.hmcts.appregister.report.normaliser;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;

@Component
public class ReportFilterNormaliser {
    public ActivityAuditFilterDto normalise(ActivityAuditFilterDto filter) {
        LocalDate dateFrom = filter.getDateFrom();
        LocalDate dateTo = filter.getDateTo();

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            filter.setDateFrom(dateTo);
            filter.setDateTo(dateFrom);
        }

        return filter;
    }

    public FeesReportFilterDto normalise(FeesReportFilterDto filter) {
        LocalDate dateFrom = filter.getDateFrom();
        LocalDate dateTo = filter.getDateTo();

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            filter.setDateFrom(dateTo);
            filter.setDateTo(dateFrom);
        }

        return filter;
    }

    public DurationFilterDto normalise(DurationFilterDto filter) {
        LocalDate dateFrom = filter.getDateFrom();
        LocalDate dateTo = filter.getDateTo();

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            filter.setDateFrom(dateTo);
            filter.setDateTo(dateFrom);
        }

        return filter;
    }
}
