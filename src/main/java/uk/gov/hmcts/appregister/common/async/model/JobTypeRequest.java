package uk.gov.hmcts.appregister.common.async.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.generated.model.JobType;

@RequiredArgsConstructor
@Getter
public class JobTypeRequest {
    private final String userName;
    private final JobType jobType;
}
