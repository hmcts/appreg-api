package uk.gov.hmcts.appregister.applicationfee.service.mapper;

import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.Fee;

@Component
@org.mapstruct.Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class FeeMapper {
    public abstract void updateFee(@MappingTarget Fee existingFee, Fee fee);
}
