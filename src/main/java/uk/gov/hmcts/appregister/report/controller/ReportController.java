package uk.gov.hmcts.appregister.report.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.TEXT_CSV;
import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.common.util.ObfuscationUtil;
import uk.gov.hmcts.appregister.generated.api.ReportsApi;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.report.service.ReportService;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {

    private final ReportService reportService;

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
        log.info("Requesting report download for job: {}", jobId);
        var reportDownload = reportService.downloadReport(jobId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + reportDownload.filename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(TEXT_CSV)
                .body(reportDownload.resource());
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
