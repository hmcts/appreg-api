package uk.gov.hmcts.appregister.job.controller;

import static uk.gov.hmcts.appregister.common.api.ApiConstants.MediaTypes.VND_JSON_V1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import uk.gov.hmcts.appregister.common.security.RoleNames;
import uk.gov.hmcts.appregister.generated.api.JobsApi;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.job.service.JobService;

@PreAuthorize(RoleNames.USER_ROLE_OR_ADMIN_ROLE_RESTRICTION)
@Controller
@RequiredArgsConstructor
public class JobApiController implements JobsApi {

    private final JobService jobService;

    @Override
    public ResponseEntity<JobAcknowledgement> getJobStatusById(UUID jobId) {
        return ResponseEntity.ok()
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(jobService.getJobAckById(jobId));
    }
}
