package uk.gov.hmcts.appregister.report.service;

import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public interface ReportService {
    JobAcknowledgement createFeesReport(FeesReportFilterDto filter);
}
