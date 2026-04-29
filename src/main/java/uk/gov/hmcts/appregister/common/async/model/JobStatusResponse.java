package uk.gov.hmcts.appregister.common.async.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.appregister.common.async.exception.JobException;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

/**
 * The representation of a job status response.
 */
@Getter
@Setter
@RequiredArgsConstructor
@Builder
public class JobStatusResponse {

    /** The job id. */
    private final UUID uuid;

    /** The job type. */
    private final JobType type;

    /** The job status. */
    private final JobStatus1 status;

    /** The user name that the job is associated with. */
    private final String userName;

    /** The error message if the job has failed. */
    private final String errorMessage;

    /** The persistence layer to use to store and read the associated clob. */
    @Getter(AccessLevel.NONE)
    protected final AsyncJobPersistenceService persistence;

    /**
     * gets the job based on the response if we ever need to lookup the state of this job again.
     *
     * @return The job id details.
     */
    public JobIdRequest getJobId() {
        return JobIdRequest.builder().id(getUuid()).userName(getUserName()).build();
    }

    /**
     * write the input stream to the clob associated with the job.
     *
     * @param updateWithInputStream The input stream to write to the clob.
     * @throws IOException Any problems
     */
    public void write(InputStream updateWithInputStream) throws IOException {
        if (status.equals(JobStatus1.FAILED) || status.equals(JobStatus1.COMPLETED)) {
            throw new JobException("Can't write clob to a finished job %s".formatted(getJobId()));
        }

        persistence.writeClob(getJobId(), updateWithInputStream);
    }

    /**
     * reads the underlying clob stream associated with the job.
     *
     * @return The clob resources. This is a spring resource that can easily be returned from the
     *     edge of the rest API.
     */
    public InputStreamResource read() throws IOException {
        return persistence.readClob(getJobId());
    }
}
