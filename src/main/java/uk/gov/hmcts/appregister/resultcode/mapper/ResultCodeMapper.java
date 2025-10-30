package uk.gov.hmcts.appregister.resultcode.mapper;

import java.time.LocalDate;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;

/**
 * MapStruct mapper for converting between {@link ResolutionCode} entities and Result Code API DTOs.
 *
 * <p>Provides two main mappings:
 *
 * <ul>
 *   <li>{@link #toDetailDto(ResolutionCode)} — produces a full DTO with code, title, wording, start
 *       and end dates.
 *   <li>{@link #toSummaryDto(ResolutionCode)} — produces a summary DTO with only code and title.
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ResultCodeMapper {

    // Map a {@link ResolutionCode} entity to a full detail DTO.
    @Mapping(target = "resultCode", source = "resultCode")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "wording", source = "wording")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    ResultCodeGetDetailDto toDetailDto(ResolutionCode entity);

    // Map a {@link ResolutionCode} entity to a summary DTO.
    @Mapping(target = "resultCode", source = "resultCode")
    @Mapping(target = "title", source = "title")
    ResultCodeGetSummaryDto toSummaryDto(ResolutionCode entity);

    default JsonNullable<LocalDate> map(LocalDate value) {
        return value != null ? JsonNullable.of(value) : JsonNullable.undefined();
    }
}
