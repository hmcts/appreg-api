package uk.gov.hmcts.appregister.job.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;

/**
 * A successful output come of {@link uk.gov.hmcts.appregister.job.validator.JobExistanceValidator}.
 */
@Getter
@RequiredArgsConstructor
@Setter
public class JobSuccess {
    /** The application list being deleted. */
    private JobStatusResponse jobStatusResponse;
}
