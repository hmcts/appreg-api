package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public interface ReportService {
    JobAcknowledgement createActivityAuditReport(ActivityAuditFilterDto filter);

    JobAcknowledgement createFeesReport(FeesReportFilterDto filter);

    JobAcknowledgement createDurationReport(DurationFilterDto filter);
}
