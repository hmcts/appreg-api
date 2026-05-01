package uk.gov.hmcts.appregister.report.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.ReportsApi;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.report.service.ReportService;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportsApi {
    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final ReportService reportService;

    @Override
    public ResponseEntity<JobAcknowledgement> createActivityAuditReport(
            ActivityAuditFilterDto activityAuditFilterDto) {
        JobAcknowledgement acknowledgement =
                reportService.createActivityAuditReport(activityAuditFilterDto);

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
        JobAcknowledgement acknowledgement = reportService.createFeesReport(feesReportFilterDto);

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
        InputStreamResource resource = reportService.getDownloadStream(jobId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .varyBy(HttpHeaders.ACCEPT)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
