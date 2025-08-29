package uk.gov.hmcts.appregister.nationalcourthouse.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;

import java.util.Optional;

/**
 * Mapper component responsible for converting {@link NationalCourtHouse} JPA entities into their
 * corresponding API-facing {@link NationalCourtHouseDto}.
 *
 * <p>This is used to decouple persistence-layer entities from the DTOs returned to API clients,
 * ensuring that:
 *
 * <ul>
 *   <li>Internal JPA annotations and lazy-loading concerns are not leaked to the outside world.
 *   <li>The response contract remains stable, even if the database schema evolves.
 * </ul>
 *
 * <p><strong>Usage:</strong> Typically invoked from the service layer after fetching {@code
 * CourtLocation} entities from the repository.
 */
@Component
public class NationalCourtHouseMapper {

    /**
     * Converts a {@link NationalCourtHouse} entity into a {@link NationalCourtHouseDto}.
     *
     * @param entity the {@link NationalCourtHouse} entity to map; may be {@code null}
     * @return a populated {@link NationalCourtHouseDto}, or {@code null} if input was {@code null}
     */
    /**
     * Converts a {@link NationalCourtHouse} entity into a {@link NationalCourtHouseDto}.
     *
     * @param entity the entity to map; may be {@code null}
     * @return an {@link Optional} containing the DTO if the entity was non-null, otherwise {@link Optional#empty()}
     */
    public Optional<NationalCourtHouseDto> toReadDto(NationalCourtHouse entity) {
        return Optional.ofNullable(entity).map(e ->
                                                   new NationalCourtHouseDto(
                                                       e.getId(),
                                                       e.getName(),
                                                       e.getCourtType(),
                                                       e.getStartDate(),
                                                       e.getEndDate(),
                                                       e.getLocationId(),
                                                       e.getPsaId(),
                                                       e.getCourtLocationCode(),
                                                       e.getWelshName(),
                                                       e.getOrgId()
                                                   )
        );
    }
}
