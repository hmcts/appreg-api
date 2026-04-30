package uk.gov.hmcts.appregister.report.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ReportsApi;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ActivityAuditReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.FeesReportParameterAudit;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.service.ReportFilterNormaliser;
import uk.gov.hmcts.appregister.report.service.ReportService;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {
    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final ReportService reportService;
    private final JobService jobService;
    private final JobMapper jobMapper;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final ReportFilterNormaliser reportFilterNormaliser;

    @Override
    public ResponseEntity<JobAcknowledgement> createActivityAuditReport(
            ActivityAuditFilterDto activityAuditFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                        unused -> {
                            ActivityAuditFilterDto normalisedFilter =
                                    reportFilterNormaliser.normalise(activityAuditFilterDto);
                            ActivityAuditReportParameterAudit reportParameterAudit =
                                    ActivityAuditReportParameterAudit.from(normalisedFilter);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportService.createActivityAuditReport(
                                                    normalisedFilter),
                                            reportParameterAudit));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return ResponseEntity.accepted()
                .location(
                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/jobs/{jobId}")
                                .buildAndExpand(acknowledgement.getId())
                                .toUri())
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(acknowledgement);
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createFeesReport(
            FeesReportFilterDto feesReportFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                        unused -> {
                            FeesReportFilterDto normalisedFilter =
                                    reportFilterNormaliser.normalise(feesReportFilterDto);
                            FeesReportParameterAudit reportParameterAudit =
                                    FeesReportParameterAudit.from(normalisedFilter);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportService.createFeesReport(normalisedFilter),
                                            reportParameterAudit));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return ResponseEntity.accepted()
                .location(
                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/jobs/{jobId}")
                                .buildAndExpand(acknowledgement.getId())
                                .toUri())
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(VND_JSON_V1)
                .body(acknowledgement);
    }

    @Override
    public ResponseEntity<Resource> downloadReport(UUID jobId) {
        var resourceHolder = new AtomicReference<InputStreamResource>();

        auditService.processAudit(
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
                            resourceHolder.set(resource);
                            return Optional.of(
                                    new AuditableResult<>("report.csv", jobMapper.toEntity(jobId)));
                        }
                    } catch (IOException e) {
                        log.error("Error reading download stream for job id: {}", jobId, e);
                        throw new AppRegistryException(
                                JobError.JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM,
                                "Download stream not available");
                    }
                },
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resourceHolder.get());
    }
}
