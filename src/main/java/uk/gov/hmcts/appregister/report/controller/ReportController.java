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
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.api.ReportsApi;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.ReportJobAudit;
import uk.gov.hmcts.appregister.report.service.ReportJobCreation;
import uk.gov.hmcts.appregister.report.service.ReportService;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {
    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");
    private static final String REPORT_DOWNLOAD_FILENAME = "report.csv";

    private final ReportService reportService;
    private final JobService jobService;
    private final AuditOperationService auditService;
    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;
    private final UserProvider userProvider;

    @Override
    public ResponseEntity<JobAcknowledgement> createActivityAuditReport(
            ActivityAuditFilterDto activityAuditFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                        unused -> {
                            ReportJobCreation reportJobCreation =
                                    reportService.createActivityAuditReport(activityAuditFilterDto);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportJobCreation.acknowledgement(),
                                            ReportJobAudit.created(
                                                    reportJobCreation.acknowledgement(),
                                                    userProvider.getUserId(),
                                                    reportJobCreation.reportParameters())));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return accepted(acknowledgement);
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createFeesReport(
            FeesReportFilterDto feesReportFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                        unused -> {
                            ReportJobCreation reportJobCreation =
                                    reportService.createFeesReport(feesReportFilterDto);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportJobCreation.acknowledgement(),
                                            ReportJobAudit.created(
                                                    reportJobCreation.acknowledgement(),
                                                    userProvider.getUserId(),
                                                    reportJobCreation.reportParameters())));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return accepted(acknowledgement);
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createDurationReport(
            DurationFilterDto durationFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT,
                        unused -> {
                            ReportJobCreation reportJobCreation =
                                    reportService.createDurationReport(durationFilterDto);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportJobCreation.acknowledgement(),
                                            ReportJobAudit.created(
                                                    reportJobCreation.acknowledgement(),
                                                    userProvider.getUserId(),
                                                    reportJobCreation.reportParameters())));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return accepted(acknowledgement);
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createListMaintenanceReport(
            ListMaintenanceFilterDto listMaintenanceFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                        unused -> {
                            ReportJobCreation reportJobCreation =
                                    reportService.createListMaintenanceReport(
                                            listMaintenanceFilterDto);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportJobCreation.acknowledgement(),
                                            ReportJobAudit.created(
                                                    reportJobCreation.acknowledgement(),
                                                    userProvider.getUserId(),
                                                    reportJobCreation.reportParameters())));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return accepted(acknowledgement);
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createPrivateProsecutorsIndexReport(
            PrivateProsecutorsIndexFilterDto privateProsecutorsIndexFilterDto) {
        JobAcknowledgement acknowledgement =
                auditService.processAudit(
                        ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                        unused -> {
                            ReportJobCreation reportJobCreation =
                                    reportService.createPrivateProsecutorsIndexReport(
                                            privateProsecutorsIndexFilterDto);
                            return Optional.of(
                                    new AuditableResult<>(
                                            reportJobCreation.acknowledgement(),
                                            ReportJobAudit.created(
                                                    reportJobCreation.acknowledgement(),
                                                    userProvider.getUserId(),
                                                    reportJobCreation.reportParameters())));
                        },
                        auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));

        return accepted(acknowledgement);
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
                                    new AuditableResult<>(
                                            REPORT_DOWNLOAD_FILENAME,
                                            ReportJobAudit.downloaded(
                                                    jobStatusResponse,
                                                    userProvider.getUserId(),
                                                    REPORT_DOWNLOAD_FILENAME)));
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
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + REPORT_DOWNLOAD_FILENAME + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resourceHolder.get());
    }

    private ResponseEntity<JobAcknowledgement> accepted(JobAcknowledgement acknowledgement) {
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
}
