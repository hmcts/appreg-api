package uk.gov.hmcts.appregister.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
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
    public ResponseEntity<Void> enableDisableDatabaseJobByName(
            AdminJobType jobName, Boolean enable) {
        adminAPIService.enableDisableDatabaseJobByName(jobName, enable);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<JobRetentionPolicy> getDatabaseJobRetentionPeriodByName(
            AdminJobType jobName) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobRetentionPeriodByName(jobName));
    }

    @Override
    public ResponseEntity<Void> updateDatabaseJobRetentionPeriodByName(
            AdminJobType jobName, Integer retentionPeriodDays) {
        adminAPIService.updateDatabaseJobRetentionPeriodByName(jobName, retentionPeriodDays);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<JobStatus> getJobStatus(AdminJobType jobType) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobStatusByName(jobType));
    }
}
