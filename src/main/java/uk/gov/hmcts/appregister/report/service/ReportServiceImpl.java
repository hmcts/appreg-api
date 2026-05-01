package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.model.JobTypeRequest;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.common.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.model.ActivityAuditReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.model.FeesReportParameterAudit;
import uk.gov.hmcts.appregister.report.job.ActivityAuditReportLifecycle;
import uk.gov.hmcts.appregister.report.job.FeesReportLifecycle;
import uk.gov.hmcts.appregister.report.job.reader.ActivityAuditReportDataReader;
import uk.gov.hmcts.appregister.report.job.reader.FeesReportDataReader;
import uk.gov.hmcts.appregister.report.normaliser.ReportFilterNormaliser;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    private final AsyncJobService asyncJobService;
    private final UserProvider userProvider;
    private final JobMapper jobMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditOperationService auditService;
    private final JobService jobService;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Value("${appreg.report.page-size}")
    private int reportPageSize;

    private final ReportFilterNormaliser reportFilterNormaliser;

    @Override
    public JobAcknowledgement createActivityAuditReport(ActivityAuditFilterDto filter) {
        return auditService.processAudit(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                unused -> {
                    ActivityAuditFilterDto normalisedFilter =
                            reportFilterNormaliser.normalise(filter);
                    ActivityAuditReportParameterAudit reportParameterAudit =
                            ActivityAuditReportParameterAudit.from(normalisedFilter);
                    return Optional.of(
                            new AuditableResult<>(
                                    runActivityAuditReport(normalisedFilter),
                                    reportParameterAudit));
                });
    }

    @Override
    public JobAcknowledgement createFeesReport(FeesReportFilterDto filter) {
        return auditService.processAudit(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                unused -> {
                    FeesReportFilterDto normalisedFilter = reportFilterNormaliser.normalise(filter);
                    FeesReportParameterAudit reportParameterAudit =
                            FeesReportParameterAudit.from(normalisedFilter);
                    return Optional.of(
                            new AuditableResult<>(
                                    runFeesReport(normalisedFilter), reportParameterAudit));
                });
    }

    @Override
    public InputStreamResource getDownloadStream(UUID jobId) {
        return auditService.processAudit(
                ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT,
                unused -> {
                    JobStatusResponse jobStatusResponse = jobService.getJobStatusById(jobId);
                    // if the job is not completed, return an error
                    if (jobStatusResponse.getStatus() != JobStatus1.COMPLETED) {
                        throw new AppRegistryException(
                                JobError.JOB_STATE_IS_NOT_SUITABLE_FOR_DOWNLOAD,
                                "Download stream not available");
                    }

                    try {
                        InputStreamResource resource = jobStatusResponse.read();

                        // if no downloadable resource is available for job, return an error
                        if (resource == null) {
                            log.error("Error reading download stream for job id: {}", jobId);
                            throw new AppRegistryException(
                                    JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM,
                                    "Download stream not available");
                        } else {
                            return Optional.of(
                                    new AuditableResult<>(resource, jobMapper.toEntity(jobId)));
                        }
                    } catch (IOException e) {
                        log.error("Error reading download stream for job id: {}", jobId, e);
                        throw new AppRegistryException(
                                JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM,
                                "Download stream not available");
                    }
                });
    }

    private JobAcknowledgement runFeesReport(FeesReportFilterDto filter) {
        FeesReportLifecycle lifecycle;
        try {
            lifecycle = new FeesReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create fees report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.FEES_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new FeesReportDataReader(jdbcTemplate, filter, schema),
                        lifecycle,
                        reportPageSize);

        return jobMapper.toDto(response);
    }

    private JobAcknowledgement runActivityAuditReport(ActivityAuditFilterDto filter) {
        ActivityAuditReportLifecycle lifecycle;
        try {
            lifecycle = new ActivityAuditReportLifecycle();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create activity audit report output file", e);
        }

        var jobRequest =
                JobTypeRequest.builder()
                        .jobType(JobType.ACTIVITY_AUDIT_REPORT)
                        .userName(userProvider.getUserId())
                        .build();

        var response =
                asyncJobService.startJob(
                        jobRequest,
                        new ActivityAuditReportDataReader(jdbcTemplate, filter, schema),
                        lifecycle,
                        reportPageSize);

        return jobMapper.toDto(response);
    }
}
