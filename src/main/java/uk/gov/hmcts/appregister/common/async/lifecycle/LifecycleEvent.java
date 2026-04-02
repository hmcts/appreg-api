package uk.gov.hmcts.appregister.common.async.lifecycle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.async.model.JobIdRequest;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class LifecycleEvent<T> {
    private final JobIdRequest response;
    private final JobType type;
    private final JobStatus status;
    private final List<T> data;
}
