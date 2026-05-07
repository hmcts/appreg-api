package uk.gov.hmcts.appregister.courtlocation.mapper;

import java.time.LocalDate;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetSummaryDto;

/**
 * MapStruct mapper for converting between {@link NationalCourtHouse} entities and Court Location
 * API DTOs.
 *
 * <p>Provides two main mappings:
 *
 * <ul>
 *   <li>{@link #toDetailDto(NationalCourtHouse)} — produces a full detail view with name, code,
 *       start and end dates.
 *   <li>{@link #toSummaryDto(NationalCourtHouse)} — produces a summary view with only name and
 *       code.
 * </ul>
 *
 * <p>The mapper is registered as a Spring bean with constructor injection. Null-handling is
 * configured to:
 *
 * <ul>
 *   <li>Always check source values before mapping ({@code nullValueCheckStrategy=ALWAYS}).
 *   <li>Ignore {@code null} values when mapping properties ({@code
 *       nullValuePropertyMappingStrategy=IGNORE}).
 * </ul>
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CourtLocationMapper {

    /**
     * Map a {@link NationalCourtHouse} entity to a full detail DTO.
     *
     * @param entity the courthouse entity
     * @return detailed Court Location DTO
     */
    @Mapping(target = "name", source = "name")
    @Mapping(target = "locationCode", source = "courtLocationCode")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    CourtLocationGetDetailDto toDetailDto(NationalCourtHouse entity);

    /**
     * Map a {@link NationalCourtHouse} entity to a summary DTO.
     *
     * @param entity the courthouse entity
     * @return summary Court Location DTO
     */
    @Mapping(target = "name", source = "name")
    @Mapping(target = "locationCode", source = "courtLocationCode")
    CourtLocationGetSummaryDto toSummaryDto(NationalCourtHouse entity);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "courtLocationCode", source = "code")
    @Mapping(target = "startDate", source = "date")
    NationalCourtHouse toEntity(String code, LocalDate date);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "courtLocationCode", source = "code", defaultValue = "")
    @Mapping(target = "name", source = "name", defaultValue = "")
    NationalCourtHouse toEntity(CodeAndName code);

    /**
     * Utility mapping method to wrap a {@link LocalDate} in a {@link JsonNullable}.
     *
     * <p>This allows optional date fields (e.g. {@code endDate}) to be properly represented in
     * generated OpenAPI DTOs where {@code null} and "undefined" must be distinguished.
     *
     * @param date the date value
     * @return a JsonNullable wrapper containing the value or null
     */
    default JsonNullable<LocalDate> map(LocalDate date) {
        return (date != null) ? JsonNullable.of(date) : JsonNullable.of(null);
    }
}
