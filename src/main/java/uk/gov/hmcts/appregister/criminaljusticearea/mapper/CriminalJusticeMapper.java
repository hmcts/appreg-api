package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

/**
 * A mapper that allows us to map from a {@link
 * uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea} entity to {@link
 * uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto} for criminal justice.
 */
@Mapper(componentModel = "spring")
public interface CriminalJusticeMapper {
    @Mapping(target = "code", source = "code")
    @Mapping(target = "description", source = "description")
    CriminalJusticeAreaGetDto toDto(CriminalJusticeArea criminalJusticeArea);
}
