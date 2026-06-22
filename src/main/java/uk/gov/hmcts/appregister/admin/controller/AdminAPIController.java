package uk.gov.hmcts.appregister.admin.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.appregister.admin.service.AdminAPIService;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.AdminApi;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@PreAuthorize(RoleNames.ADMIN_ROLE_RESTRICTION)
@Validated
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminAPIController implements AdminApi {

    private final AdminAPIService adminAPIService;

    @Override
    @PutMapping(
            value = PATH_ENABLE_DISABLE_DATABASE_JOB_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> enableDisableDatabaseJobByName(
            @PathVariable("jobType") AdminJobType jobName, @RequestParam("enable") Boolean enable) {
        adminAPIService.enableDisableDatabaseJobByName(jobName, enable);
        return ResponseEntity.ok().varyBy("Accept").contentType(VND_JSON_V1).build();
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
        return ResponseEntity.ok().varyBy("Accept").contentType(VND_JSON_V1).build();
    }

    @Override
    @GetMapping(
            value = PATH_GET_JOB_STATUS,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<JobStatus> getJobStatus(@PathVariable("jobType") AdminJobType jobType) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobStatusByName(jobType));
    }
}
