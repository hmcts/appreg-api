package uk.gov.hmcts.appregister.common.async.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.generated.model.JobType;

@RequiredArgsConstructor
@Getter
@Builder
public class JobTypeRequest {
    private final String userName;
    private final JobType jobType;
}
