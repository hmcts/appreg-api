package uk.gov.hmcts.appregister.common.async.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

/**
 * This mapper works for the asynchronous job status mapper.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class JobStatusMapper {

    /**
     * Maps the response status.
     *
     * @param status The status to map
     * @return The database status
     */
    public JobStatusType getJobStatus(JobStatus1 status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case RECEIVED -> JobStatusType.SUBMITTED;
            case VALIDATING -> JobStatusType.PENDING;
            case COMPLETED -> JobStatusType.COMPLETED;
            case FAILED -> JobStatusType.FAILED;
            case PROCESSING -> JobStatusType.RUNNING;
        };
    }

    /**
     * Maps the response status.
     *
     * @param status The database status to map
     * @return The status
     */
    public JobStatus1 getJobStatus(JobStatusType status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING -> JobStatus1.VALIDATING;
            case SUBMITTED -> JobStatus1.RECEIVED;
            case COMPLETED -> JobStatus1.COMPLETED;
            case FAILED -> JobStatus1.FAILED;
            case RUNNING -> JobStatus1.PROCESSING;
        };
    }
}
