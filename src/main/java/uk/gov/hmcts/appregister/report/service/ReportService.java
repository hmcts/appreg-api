package uk.gov.hmcts.appregister.report.service;

import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

public interface ReportService {

    /**
     * runs the activity audit report.
     *
     * @param filter The filter criteria to limit data set being reported on
     * @return The job acknowledgement
     */
    JobAcknowledgement createActivityAuditReport(ActivityAuditFilterDto filter);

    /**
     * runs the fee report.
     *
     * @param filter The filter criteria to limit data set being reported on
     * @return The job acknowledgement
     */
    JobAcknowledgement createFeesReport(FeesReportFilterDto filter);

    /**
     * get download stream for job id.
     *
     * @param jobId The job id to get the download stream for
     * @return The download stream
     */
    InputStreamResource getDownloadStream(UUID jobId);
}
