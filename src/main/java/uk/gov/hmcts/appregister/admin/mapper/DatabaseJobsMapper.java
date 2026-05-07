package uk.gov.hmcts.appregister.admin.mapper;

import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
public abstract class DatabaseJobsMapper {

    @Mapping(target = "lastRan", source = "job.lastRan")
    @Mapping(target = "enabled", source = "job.enabled")
    public abstract JobStatus toDatabaseJobStatus(DatabaseJob job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "lastRan", ignore = true)
    @Mapping(target = "name", source = "jobType.value")
    public abstract DatabaseJob toEntity(AdminJobType jobType);

    public Boolean map(YesOrNo enabled) {
        return enabled.isYes();
    }
}
