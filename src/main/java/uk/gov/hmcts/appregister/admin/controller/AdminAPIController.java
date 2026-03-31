package uk.gov.hmcts.appregister.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import uk.gov.hmcts.appregister.admin.service.AdminAPIService;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.AdminApi;
import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

@PreAuthorize(RoleNames.ADMIN_ROLE_RESTRICTION)
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminAPIController implements AdminApi {

    private final AdminAPIService adminAPIService;

    @Override
    public ResponseEntity<DatabaseJobStatus> getDatabaseJobStatusByName(String jobName) {
        return ResponseEntity.ok(adminAPIService.getDatabaseJobStatusByName(jobName));
    }

    @Override
    public ResponseEntity<Void> enableDisableDatabaseJobByName(String jobName, Boolean enable) {
        adminAPIService.enableDisableDatabaseJobByName(jobName, enable);
        return ResponseEntity.ok().build();
    }
}
