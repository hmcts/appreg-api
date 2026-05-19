package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;

public interface ReportService {
    ReportJobCreation createActivityAuditReport(ActivityAuditFilterDto filter);

    ReportJobCreation createFeesReport(FeesReportFilterDto filter);

    ReportJobCreation createSearchWarrantsReport(SearchWarrantsReportFilterDto filter);

    ReportJobCreation createDurationReport(DurationFilterDto filter);

    ReportJobCreation createListMaintenanceReport(ListMaintenanceFilterDto filter);

    ReportJobCreation createPrivateProsecutorsIndexReport(PrivateProsecutorsIndexFilterDto filter);
}
