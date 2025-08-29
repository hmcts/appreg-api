package uk.gov.hmcts.appregister.nationalcourthouse.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;

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
    public NationalCourtHouseDto toReadDto(NationalCourtHouse entity) {
        if (entity == null) {
            // Defensive null check to avoid NullPointerExceptions.
            // Returning null here lets the service/controller decide
            // how to handle missing entities.
            return null;
        }

        // Map entity fields directly to the DTO record.
        // Each field corresponds to a column in the national_court_houses table,
        // except where the DTO normalises naming for client readability.
        return new NationalCourtHouseDto(
                entity.getId(), // primary key ID
                entity.getName(), // courthouse name
                entity.getCourtType(), // type of court (e.g. CROWN, MAGISTRATES)
                entity.getStartDate(), // validity start date
                entity.getEndDate(), // validity end date (nullable)
                entity.getLocationId(), // FK to location
                entity.getPsaId(), // FK to petty sessions area
                entity.getCourtLocationCode(), // business reference code
                entity.getWelshName(), // Welsh-language name, if defined
                entity.getOrgId() // organisation ID
                );
    }
}
