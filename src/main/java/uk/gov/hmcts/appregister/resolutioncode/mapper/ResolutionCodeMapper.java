package uk.gov.hmcts.appregister.resolutioncode.mapper;

import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

/**
 * Mapper for converting between {@link ResolutionCode} entities and API-facing DTOs.
 *
 * <p><strong>Purpose:</strong>
 *
 * <ul>
 *   <li>Prevents persistence-layer concerns (JPA annotations, column names) from leaking into API
 *       responses.
 *   <li>Provides full {@link ResolutionCodeDto} objects for detail views.
 *   <li>Provides lightweight {@link ResolutionCodeListItemDto} projections for search/list
 *       endpoints.
 *   <li>Supports mapping DTOs back into entities for use in tests or service-layer operations.
 * </ul>
 *
 * <p><strong>Optional return types:</strong> All methods return {@code Optional<T>} to make the
 * possibility of {@code null} input explicit. Callers must unwrap the value or handle the empty
 * case. This avoids silent {@code null} values and forces the service layer to decide whether to
 * skip or fail fast when mapping is not possible.
 */
@Component
public class ResolutionCodeMapper {

    /**
     * Maps a {@link ResolutionCode} entity into a full {@link ResolutionCodeDto}.
     *
     * @param entity the JPA entity to map; may be {@code null}
     * @return an {@link Optional} containing the mapped DTO, or empty if {@code entity} was null
     */
    public Optional<ResolutionCodeDto> toReadDto(ResolutionCode entity) {
        return Optional.ofNullable(entity)
                .map(
                        e ->
                                new ResolutionCodeDto(
                                        e.getId(),
                                        e.getResultCode(),
                                        e.getTitle(),
                                        e.getWording(),
                                        e.getLegislation(),
                                        e.getDestinationEmail1(),
                                        e.getDestinationEmail2(),
                                        e.getStartDate(),
                                        e.getEndDate()));
    }

    /**
     * Maps a {@link ResolutionCodeDto} back into a {@link ResolutionCode} entity.
     *
     * <p>This is primarily useful in service or test code, not typically exposed to API clients.
     *
     * @param dto the DTO to map; may be {@code null}
     * @return an {@link Optional} containing the mapped entity, or empty if {@code dto} was null
     */
    public Optional<ResolutionCode> toEntityFromReadDto(ResolutionCodeDto dto) {
        return Optional.ofNullable(dto)
                .map(
                        d ->
                                ResolutionCode.builder()
                                        .id(d.id())
                                        .resultCode(d.resultCode())
                                        .title(d.title())
                                        .wording(d.wording())
                                        .legislation(d.legislation())
                                        .destinationEmail1(d.destinationEmail1())
                                        .destinationEmail2(d.destinationEmail2())
                                        .startDate(d.startDate())
                                        .endDate(d.endDate())
                                        .build());
    }

    /**
     * Maps a {@link ResolutionCode} entity into a lightweight {@link ResolutionCodeListItemDto}.
     *
     * <p>Intended for use in paginated search endpoints where only id, code, and title are
     * required.
     *
     * @param entity the JPA entity to map; may be {@code null}
     * @return an {@link Optional} containing the list item DTO, or empty if {@code entity} was null
     */
    public Optional<ResolutionCodeListItemDto> toListItem(ResolutionCode entity) {
        return Optional.ofNullable(entity)
                .map(
                        e ->
                                new ResolutionCodeListItemDto(
                                        e.getId(), e.getResultCode(), e.getTitle()));
    }
}
