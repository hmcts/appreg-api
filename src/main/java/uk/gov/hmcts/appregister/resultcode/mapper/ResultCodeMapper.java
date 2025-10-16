package uk.gov.hmcts.appregister.resultcode.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import org.openapitools.jackson.nullable.JsonNullable;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ResultCodeGetSummaryDto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /**
     * Utility mapping method to wrap a {@link LocalDateTime} in a {@link JsonNullable<LocalDate>}.
     *
     * <p>Converts the timestamp to a date (drops time component) and wraps it in a JsonNullable
     * so that OpenAPI DTOs can distinguish between {@code null} and undefined fields.
     *
     * @param dateTime the LocalDateTime value from the entity
     * @return a JsonNullable containing the corresponding LocalDate, or an empty JsonNullable if null
     */
    default JsonNullable<LocalDate> map(LocalDateTime dateTime) {
        return dateTime != null
            ? JsonNullable.of(dateTime.toLocalDate())
            : JsonNullable.undefined();
    }
}
