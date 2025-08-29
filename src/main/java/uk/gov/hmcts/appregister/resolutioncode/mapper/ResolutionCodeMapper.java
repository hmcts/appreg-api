package uk.gov.hmcts.appregister.resolutioncode.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

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
 *   <li>Map a {@link ResolutionCode} entity into a lightweight {@link ResolutionCodeListItemDto} containing
 *       only fields relevant for list views.
 * </ul>
 */
@Component
public class ResolutionCodeMapper {

    /**
     * Converts a {@link ResolutionCode} entity into a {@link ResolutionCodeDto}.
     *
     * @param entity the entity to map; may be {@code null}
     * @return a populated {@link ResolutionCodeDto}, or {@code null} if the input was null
     */
    public ResolutionCodeDto toReadDto(ResolutionCode entity) {
        if (entity == null) {
            // Defensive: return null if no entity provided.
            return null;
        }

        // Map all fields from the entity into the full DTO.
        return new ResolutionCodeDto(
                entity.getId(),
                entity.getResultCode(),
                entity.getTitle(),
                entity.getWording(),
                entity.getLegislation(),
                entity.getDestinationEmail1(),
                entity.getDestinationEmail2(),
                entity.getStartDate(),
                entity.getEndDate());
    }

    /**
     * Converts a {@link ResolutionCodeDto} back into a {@link ResolutionCode} entity.
     *
     * <p>This is primarily useful in test code, or where you need to persist a DTO back to the
     * database via the repository.
     *
     * @param dto the DTO to map; may be {@code null}
     * @return a new {@link ResolutionCode} entity built from the DTO fields, or {@code null} if input
     *     was null
     */
    public ResolutionCode toEntityFromReadDto(ResolutionCodeDto dto) {
        if (dto == null) {
            // Defensive: return null if no DTO provided.
            return null;
        }

        // Use Lombok-generated builder on the entity to map fields.
        return ResolutionCode.builder()
                .id(dto.id())
                .resultCode(dto.resultCode())
                .title(dto.title())
                .wording(dto.wording())
                .legislation(dto.legislation())
                .destinationEmail1(dto.destinationEmail1())
                .destinationEmail2(dto.destinationEmail2())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .build();
    }

    /**
     * Converts a {@link ResolutionCode} entity into a lightweight {@link ResolutionCodeListItemDto}.
     *
     * <p>This variant is intended for paginated list/search responses, where only ID, code, and
     * title are required.
     *
     * @param entity the entity to map; may be {@code null}
     * @return a {@link ResolutionCodeListItemDto} containing only ID, code, and title, or {@code null}
     *     if input was null
     */
    public ResolutionCodeListItemDto toListItem(ResolutionCode entity) {
        if (entity == null) {
            // Defensive: return null if no entity provided.
            return null;
        }

        // Map only a subset of fields.
        return new ResolutionCodeListItemDto(entity.getId(), entity.getResultCode(), entity.getTitle());
    }
}
