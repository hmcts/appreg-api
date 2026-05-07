package uk.gov.hmcts.appregister.job.validator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobService;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * A validator that checks if the requested job exists for this user.
 */
@Component
@RequiredArgsConstructor
public class JobExistanceValidator implements Validator<UUID, JobSuccess> {
    private final AsyncJobService asyncJobService;

    private final UserProvider userProvider;

    @Override
    public void validate(UUID request) {
        validate(request, (uuid, jobSuccess) -> null);
    }

    @Override
    public <R> R validate(UUID uuid, BiFunction<UUID, JobSuccess, R> validateFunction) {
        Optional<JobStatusResponse> jobStatusResponse =
                asyncJobService.getJobStatus(
                        JobIdRequest.builder().id(uuid).userName(userProvider.getUserId()).build());

        if (jobStatusResponse.isEmpty()) {
            throw new AppRegistryException(
                    JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER,
                    "Job type with id: " + uuid + " does not exist or is not for user");
        }

        JobSuccess jobSuccess = new JobSuccess();
        jobSuccess.setJobStatusResponse(jobStatusResponse.get());
        return validateFunction.apply(uuid, jobSuccess);
    }
}
