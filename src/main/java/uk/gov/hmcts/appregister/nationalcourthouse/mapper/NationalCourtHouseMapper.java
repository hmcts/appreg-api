package uk.gov.hmcts.appregister.nationalcourthouse.mapper;

import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;

/**
 * Mapper component responsible for converting {@link NationalCourtHouse} JPA entities into their
 * corresponding API-facing {@link NationalCourtHouseDto}.
 *
 * <p>This ensures persistence-layer concerns (JPA annotations, lazy-loading, etc.) are not leaked
 * to API clients, and that the API contract remains stable even if the underlying schema changes.
 *
 * <p><strong>Usage:</strong> Typically invoked from the service layer after fetching {@link
 * NationalCourtHouse} entities from the repository.
 */
@Component
public class NationalCourtHouseMapper {

    /**
     * Converts a {@link NationalCourtHouse} entity into a {@link NationalCourtHouseDto}.
     *
     * <p>This method returns an {@link Optional} so that callers must handle the case where the
     * provided entity is {@code null}.
     *
     * @param entity the entity to map; may be {@code null}
     * @return an {@link Optional} containing the DTO if the entity was non-null, otherwise {@link
     *     Optional#empty()}
     */
    public Optional<NationalCourtHouseDto> toReadDto(NationalCourtHouse entity) {
        return Optional.ofNullable(entity)
                .map(
                        e ->
                                new NationalCourtHouseDto(
                                        e.getId(),
                                        e.getName(),
                                        e.getCourtType(),
                                        e.getStartDate(),
                                        e.getEndDate(),
                                        e.getLocationId(),
                                        e.getPsaId() != null ? e.getPsaId().getId() : null,
                                        e.getCourtLocationCode(),
                                        e.getWelshName(),
                                        e.getOrgId()));
    }
}
