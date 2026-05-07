package uk.gov.hmcts.appregister.job.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    private static final MediaType VND_JSON_V1 =
            MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");

    @Override
    public ResponseEntity<JobAcknowledgement> getJobStatusById(UUID jobId) {
        return ResponseEntity.ok()
                .varyBy("Accept")
                .contentType(VND_JSON_V1)
                .body(jobService.getJobAckById(jobId));
    }
}
