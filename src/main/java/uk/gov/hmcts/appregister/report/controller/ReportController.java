package uk.gov.hmcts.appregister.report.controller;

import java.io.IOException;
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
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.service.AuditOperationService;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;
import uk.gov.hmcts.appregister.generated.api.ReportsApi;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.job.service.JobService;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.audit.ReportJobAudit;
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
    private final UserProvider userProvider;

    @Override
    public ResponseEntity<JobAcknowledgement> createActivityAuditReport(
            ActivityAuditFilterDto activityAuditFilterDto) {
        log.info(
                "Activity Audit Report payload: {}",
                ObfuscationUtil.getObfuscatedString(activityAuditFilterDto));
        return accepted(
                reportService.createActivityAuditReport(activityAuditFilterDto).acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createFeesReport(
            FeesReportFilterDto feesReportFilterDto) {
        log.info(
                "Fees report payload: {}",
                ObfuscationUtil.getObfuscatedString(feesReportFilterDto));
        return accepted(reportService.createFeesReport(feesReportFilterDto).acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createWorkloadReport(
            WorkloadFilterDto workloadFilterDto) {
        log.info(
                "Workload report payload: {}",
                ObfuscationUtil.getObfuscatedString(workloadFilterDto));
        return accepted(reportService.createWorkloadReport(workloadFilterDto).acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createSearchWarrantsReport(
            SearchWarrantsReportFilterDto searchWarrantsReportFilterDto) {
        log.info(
                "Search warrants report payload: {}",
                ObfuscationUtil.getObfuscatedString(searchWarrantsReportFilterDto));
        return accepted(
                reportService
                        .createSearchWarrantsReport(searchWarrantsReportFilterDto)
                        .acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createDurationReport(
            DurationFilterDto durationFilterDto) {
        log.info(
                "Duration report payload: {}",
                ObfuscationUtil.getObfuscatedString(durationFilterDto));
        return accepted(reportService.createDurationReport(durationFilterDto).acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createListMaintenanceReport(
            ListMaintenanceFilterDto listMaintenanceFilterDto) {
        log.info(
                "List maintenance report payload: {}",
                ObfuscationUtil.getObfuscatedString(listMaintenanceFilterDto));
        return accepted(
                reportService
                        .createListMaintenanceReport(listMaintenanceFilterDto)
                        .acknowledgement());
    }

    @Override
    public ResponseEntity<JobAcknowledgement> createPrivateProsecutorsIndexReport(
            PrivateProsecutorsIndexFilterDto privateProsecutorsIndexFilterDto) {
        log.info(
                "Private Prosecutors Index report payload: {}",
                ObfuscationUtil.getObfuscatedString(privateProsecutorsIndexFilterDto));
        return accepted(
                reportService
                        .createPrivateProsecutorsIndexReport(privateProsecutorsIndexFilterDto)
                        .acknowledgement());
    }

    @Override
    public ResponseEntity<Resource> downloadReport(UUID jobId) {
        var resourceHolder = new AtomicReference<InputStreamResource>();

        log.info("Requesting report download for job: {}", jobId);

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
                });

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
        log.info("Job acknowledgement: {}", acknowledgement);
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
