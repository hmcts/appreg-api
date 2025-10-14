package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/**
 * Mapper for StandardApplicant entity to StandardApplicantDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StandardApplicantMapper {
    @Mapping(target = "applicantName", source = "name")
    StandardApplicantDto toReadDto(StandardApplicant entity);
}
