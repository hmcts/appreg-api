package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;

public interface ReportService {
    ReportJobCreation createActivityAuditReport(ActivityAuditFilterDto filter);

    ReportJobCreation createFeesReport(FeesReportFilterDto filter);

    ReportJobCreation createDurationReport(DurationFilterDto filter);

    ReportJobCreation createPrivateProsecutorsIndexReport(PrivateProsecutorsIndexFilterDto filter);
}
