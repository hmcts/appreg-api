package uk.gov.hmcts.appregister.resolutioncode.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;

/**
 * MapStruct mapper for converting between {@link ResolutionCode} entities and Result Code
 * API DTOs.
 *
 * <p>Provides two main mappings:
 *
 * <ul>
 *   <li>{@link #toDetailDto(ResolutionCode)} — produces a full DTO with code, title, wording,
 *       start and end dates.
 *   <li>{@link #toSummaryDto(ResolutionCode)} — produces a summary DTO with only code and
 *       title.
 * </ul>
 *
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ResultCodeMapper {

    /**
     * Map a {@link ResolutionCode} entity to a full detail DTO.
     *
     * @param entity the ResolutionCode entity
     * @return detailed ResultCode DTO
     */
    @Mapping(target = "resultCode", source = "resultCode")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "wording", source = "wording")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    ResultCodeGetDetailDto toDetailDto(ResolutionCode entity);

    /**
     * Map a {@link ResolutionCode} entity to a summary DTO.
     *
     * @param entity the ResolutionCode entity
     * @return summary ResultCode DTO
     */
    @Mapping(target = "resultCode", source = "resultCode")
    @Mapping(target = "title", source = "title")
    ResultCodeGetSummaryDto toSummaryDto(ResolutionCode entity);
}
