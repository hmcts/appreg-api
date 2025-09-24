package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto;

/**
 * A mapper that allows us to map from a {@link
 * uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea} entity to {@link
 * uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaDto} for criminal justice.
 */
@Mapper(componentModel = "spring")
public interface CriminalJusticeMapper {
    @Mapping(target = "code", source = "cjaCode")
    @Mapping(target = "description", source = "cjaDescription")
    CriminalJusticeAreaDto toDto(CriminalJusticeArea criminalJusticeArea);
}
