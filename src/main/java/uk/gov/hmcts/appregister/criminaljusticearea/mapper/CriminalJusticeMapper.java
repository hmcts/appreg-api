package uk.gov.hmcts.appregister.criminaljusticearea.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.mapper.OutgoingDtoSanitiser;
import uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto;

/**
 * A mapper that allows us to map from a {@link
 * uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea} entity to {@link
 * uk.gov.hmcts.appregister.generated.model.CriminalJusticeAreaGetDto} for criminal justice.
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CriminalJusticeMapper {
    @Mapping(target = "code", source = "code")
    @Mapping(target = "description", source = "description")
    CriminalJusticeAreaGetDto toDto(CriminalJusticeArea criminalJusticeArea);

    @AfterMapping
    default void sanitizeDto(@MappingTarget CriminalJusticeAreaGetDto target) {
        OutgoingDtoSanitiser.sanitize(target);
    }

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "description", source = "description")
    CriminalJusticeArea toEntity(CodeAndDescription codeAndDescription);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "description", ignore = true)
    CriminalJusticeArea toEntity(String code);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCJA(@MappingTarget CriminalJusticeArea existingCJA, CriminalJusticeArea newCJA);
}
