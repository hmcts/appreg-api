package uk.gov.hmcts.appregister.common.async.mapper;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * This mapper works for the asynchronous job status mapper.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
@Slf4j
public abstract class JobStatusMapper {

    /**
     * Maps the response status.
     *
     * @param status The status to map
     * @return The database status
     */
    public JobStatusType getJobStatus(JobStatus1 status) {
        if (status == JobStatus1.RECEIVED) {
            return JobStatusType.SUBMITTED;
        } else if (status == JobStatus1.VALIDATING) {
            return JobStatusType.PENDING;
        } else if (status == JobStatus1.COMPLETED) {
            return JobStatusType.COMPLETED;
        } else if (status == JobStatus1.FAILED) {
            return JobStatusType.FAILED;
        } else if (status == JobStatus1.PROCESSING) {
            return JobStatusType.RUNNING;
        }

        return null;
    }

    /**
     * Maps the response status.
     *
     * @param status The database status to map
     * @return The status
     */
    public JobStatus1 getJobStatus(JobStatusType status) {
        if (status == JobStatusType.PENDING) {
            return JobStatus1.VALIDATING;
        } else if (status == JobStatusType.SUBMITTED) {
            return JobStatus1.RECEIVED;
        } else if (status == JobStatusType.COMPLETED) {
            return JobStatus1.COMPLETED;
        } else if (status == JobStatusType.FAILED) {
            return JobStatus1.FAILED;
        } else if (status == JobStatusType.RUNNING) {
            return JobStatus1.PROCESSING;
        }

        return null;
    }
}
