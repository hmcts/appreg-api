package uk.gov.hmcts.appregister.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
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
    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    private final AdminAPIService adminAPIService;

    @Override
    @PutMapping(
            value = AdminApi.PATH_ENABLE_DISABLE_DATABASE_JOB_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> enableDisableDatabaseJobByName(
            AdminJobType jobName, Boolean enable) {
        adminAPIService.enableDisableDatabaseJobByName(jobName, enable);
        return ResponseEntity.ok().varyBy("Accept").contentType(VND_JSON_V1).build();
    }

    @Override
    public ResponseEntity<JobRetentionPolicy> getDatabaseJobRetentionPeriodByName(
            AdminJobType jobName) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobRetentionPeriodByName(jobName));
    }

    @Override
    @PutMapping(
            value = AdminApi.PATH_UPDATE_DATABASE_JOB_RETENTION_PERIOD_BY_NAME,
            produces = {"application/vnd.hmcts.appreg.v1+json", "application/problem+json"})
    public ResponseEntity<Void> updateDatabaseJobRetentionPeriodByName(
            AdminJobType jobName, Integer retentionPeriodDays) {
        adminAPIService.updateDatabaseJobRetentionPeriodByName(jobName, retentionPeriodDays);
        return ResponseEntity.ok().varyBy("Accept").contentType(VND_JSON_V1).build();
    }

    @Override
    public ResponseEntity<JobStatus> getJobStatus(AdminJobType jobType) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobStatusByName(jobType));
    }
}
