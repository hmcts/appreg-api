package uk.gov.hmcts.appregister.admin.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.appregister.admin.service.AdminAPIService;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngestService;
import uk.gov.hmcts.appregister.generated.api.AdminApi;
import uk.gov.hmcts.appregister.generated.model.AdminJobStatus;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;

@PreAuthorize(RoleNames.ADMIN_ROLE_RESTRICTION)
@Validated
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminAPIController implements AdminApi {
    private static final String VARY_ACCEPT = HttpHeaders.ACCEPT;

    private final AdminAPIService adminAPIService;
    private final CsdsIngestService csdsIngestService;
    private final CsdsIngressProcessor csdsIngressProcessor;

    @Override
    @PutMapping(
            value = PATH_ENABLE_DISABLE_DATABASE_JOB_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> enableDisableDatabaseJobByName(
            @PathVariable("jobType") AdminJobType jobName, @RequestParam("enable") Boolean enable) {
        adminAPIService.enableDisableDatabaseJobByName(jobName, enable);
        return ResponseEntity.ok().varyBy(VARY_ACCEPT).contentType(VND_JSON_V1).build();
    }

    @Override
    @GetMapping(
            value = PATH_GET_DATABASE_JOB_RETENTION_PERIOD_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<JobRetentionPolicy> getDatabaseJobRetentionPeriodByName(
            @PathVariable("jobType") AdminJobType jobName) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobRetentionPeriodByName(jobName));
    }

    @Override
    @PutMapping(
            value = PATH_UPDATE_DATABASE_JOB_RETENTION_PERIOD_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> updateDatabaseJobRetentionPeriodByName(
            @PathVariable("jobType") AdminJobType jobName,
            @RequestParam("retentionPeriodDays") Integer retentionPeriodDays) {
        adminAPIService.updateDatabaseJobRetentionPeriodByName(jobName, retentionPeriodDays);
        return ResponseEntity.ok().varyBy(VARY_ACCEPT).contentType(VND_JSON_V1).build();
    }

    @Override
    @GetMapping(
            value = PATH_GET_JOB_STATUS,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<AdminJobStatus> getJobStatus(
            @PathVariable("jobType") AdminJobType jobType) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobStatusByName(jobType));
    }

    @Override
    @PostMapping(
            value = PATH_INGEST_CSDS_DATA,
            consumes = {"multipart/form-data"},
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<CsdsIngestResponse> ingestCsdsData(
            @PathVariable("processor") String processor,
            @RequestPart(value = "file", required = true) MultipartFile file) {
        return ResponseEntity.ok()
                .varyBy(VARY_ACCEPT)
                .contentType(VND_JSON_V1)
                .body(csdsIngestService.ingest(processor, file));
    }

    @Override
    @PostMapping(
            value = PATH_TRIGGER_CSDS_INGRESS,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> triggerCsdsIngress() {
        csdsIngressProcessor.runManualIngress();
        return ResponseEntity.ok().varyBy(VARY_ACCEPT).contentType(VND_JSON_V1).build();
    }
}
