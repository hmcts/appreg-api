package uk.gov.hmcts.appregister.resolutioncode.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

import java.util.Optional;

/**
 * Mapper component responsible for converting between JPA {@link ResolutionCode} entities and their
 * corresponding API-facing DTOs.
 *
 * <p>This class ensures that persistence-layer entities (with JPA annotations and internal schema
 * concerns) are not leaked to API clients. It also provides smaller DTOs for specific use cases
 * such as list views.
 *
 * <p><strong>Responsibilities:</strong>
 *
 * <ul>
 *   <li>Map a full {@link ResolutionCode} entity into a complete {@link ResolutionCodeDto}.
 *   <li>Reconstruct a {@link ResolutionCode} entity from a {@link ResolutionCodeDto}, e.g. for
 *       creating/updating in service or test code.
 *   <li>Map a {@link ResolutionCode} entity into a lightweight {@link ResolutionCodeListItemDto}
 *       containing only fields relevant for list views.
 * </ul>
 */
@Component
public class ResolutionCodeMapper {

    /**
     * Converts a {@link ResolutionCode} entity into a {@link ResolutionCodeDto}.
     *
     * @param entity the entity to map; may be {@code null}
     * @return an {@link Optional} containing the DTO if the entity was non-null, otherwise empty
     */
    public Optional<ResolutionCodeDto> toReadDto(ResolutionCode entity) {
        return Optional.ofNullable(entity).map(e -> new ResolutionCodeDto(
            e.getId(),
            e.getResultCode(),
            e.getTitle(),
            e.getWording(),
            e.getLegislation(),
            e.getDestinationEmail1(),
            e.getDestinationEmail2(),
            e.getStartDate(),
            e.getEndDate()
        ));
    }

    /**
     * Converts a {@link ResolutionCodeDto} back into a {@link ResolutionCode} entity.
     *
     * @param dto the DTO to map; may be {@code null}
     * @return an {@link Optional} containing the entity if the DTO was non-null, otherwise empty
     */
    public Optional<ResolutionCode> toEntityFromReadDto(ResolutionCodeDto dto) {
        return Optional.ofNullable(dto).map(d -> ResolutionCode.builder().id(d.id()).resultCode(d.resultCode()).title(d.title()).wording(
            d.wording()).legislation(d.legislation()).destinationEmail1(d.destinationEmail1()).destinationEmail2(d.destinationEmail2()).startDate(
            d.startDate()).endDate(d.endDate()).build());
    }

    /**
     * Converts a {@link ResolutionCode} entity into a lightweight {@link ResolutionCodeListItemDto}.
     *
     * @param entity the entity to map; may be {@code null}
     * @return an {@link Optional} containing the list item DTO if the entity was non-null, otherwise empty
     */
    public Optional<ResolutionCodeListItemDto> toListItem(ResolutionCode entity) {
        return Optional.ofNullable(entity).map(e -> new ResolutionCodeListItemDto(
            e.getId(),
                                                                                  e.getResultCode(),
                                                                                  e.getTitle()
        ));
    }
}
