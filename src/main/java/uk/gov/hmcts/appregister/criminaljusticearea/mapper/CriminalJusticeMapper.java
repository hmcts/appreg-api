package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

/** A mapped that allows us to map from entity to dto for criminal justice. */
@Mapper(componentModel = "spring")
public interface CriminalJusticeMapper {
    @Mapping(target = "code", source = "cjaCode")
    @Mapping(target = "description", source = "cjaDescription")
    CriminalJusticeAreaDto toDto(CriminalJusticeArea criminalJusticeArea);
}
