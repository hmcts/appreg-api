package uk.gov.hmcts.appregister.admin.mapper;

import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
public abstract class DatabaseJobsMapper {

    @Mapping(target = "lastRan", source = "job.lastRan")
    @Mapping(target = "enabled", source = "job.enabled")
    public abstract DatabaseJobStatus toDatabaseJobStatus(DatabaseJob job);

    public Boolean map(YesOrNo enabled) {
        return enabled.isYes();
    }
}
