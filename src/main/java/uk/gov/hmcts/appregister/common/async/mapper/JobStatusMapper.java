package uk.gov.hmcts.appregister.common.async.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

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
    public JobStatusType getJobStatus(JobStatus status) {
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
    public JobStatus getJobStatus(JobStatusType status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING -> JobStatus.VALIDATING;
            case SUBMITTED -> JobStatus.RECEIVED;
            case COMPLETED -> JobStatus.COMPLETED;
            case FAILED -> JobStatus.FAILED;
            case RUNNING -> JobStatus.PROCESSING;
        };
    }
}
