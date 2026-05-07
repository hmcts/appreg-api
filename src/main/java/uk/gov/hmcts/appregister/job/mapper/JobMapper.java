package uk.gov.hmcts.appregister.job.mapper;

import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface JobMapper {
    @Mapping(target = "status", source = "jobStatusResponse.status")
    @Mapping(target = "id", source = "jobStatusResponse.uuid")
    @Mapping(target = "type", source = "jobStatusResponse.type")
    @Mapping(target = "errorDescription", source = "jobStatusResponse.errorMessage")
    JobAcknowledgement toDto(JobStatusResponse jobStatusResponse);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "uuid", source = "jobId")
    @Mapping(target = "jobState", ignore = true)
    @Mapping(target = "jobType", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "failureMessage", ignore = true)
    @Mapping(target = "userName", ignore = true)
    AsyncJob toEntity(UUID jobId);
}
