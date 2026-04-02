package uk.gov.hmcts.appregister.common.async.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.async.JobStatusPersistence;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class JobStatusResponse {
    private final UUID uuid;
    private final JobType type;
    private final JobStatus status;
    private final String userName;
    private final JobStatusPersistence persistence;

    public JobIdRequest getJobId() {
        return JobIdRequest.builder().id(getUuid().toString()).userName(getUserName()).build();
    }

    public void write(InputStream updateWithInputStream) {
         persistence.writeBlob(getJobId(), updateWithInputStream);
    }

    public void read(OutputStream updateWithInputStream) {
        persistence.readBlob(getJobId(), updateWithInputStream);
    }
}
