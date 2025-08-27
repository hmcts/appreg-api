package uk.gov.hmcts.appregister.courtlocation.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;

/**
 * Service layer contract for interacting with Court Location data.
 *
 * <p>This interface defines the operations available for reading Court Location records, decoupling
 * the controller from the repository layer. Implementations are responsible for applying business
 * rules, mapping JPA entities to DTOs, and delegating persistence operations to a repository.
 *
 * <p>Usage pattern:
 *
 * <ul>
 *   <li>Controllers depend on this interface rather than directly on repositories.
 *   <li>Implementations typically use {@link
 *       uk.gov.hmcts.appregister.courtlocation.repository.CourtLocationRepository} and {@link
 *       uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapper}.
 * </ul>
 */
public interface CourtLocationService {

    /**
     * Fetch all court locations without pagination or filtering.
     *
     * @return a complete list of {@link CourtLocationDto} objects; may be empty if no court
     *     locations exist
     */
    List<CourtLocationDto> findAll();

    /**
     * Find a single court location by its identifier.
     *
     * @param id the unique identifier of the court location
     * @return the corresponding {@link CourtLocationDto}
     * @throws org.springframework.web.server.ResponseStatusException with status 404 (NOT_FOUND) if
     *     no court location exists for the given id
     */
    CourtLocationDto findById(Long id);

    /**
     * Search for court locations using optional filters and pagination.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code name} – optional case-insensitive substring filter on the courthouse name.
     *   <li>{@code courtType} – optional exact match on the court type field.
     * </ul>
     *
     * <p>Paging and sorting are handled via the provided {@link Pageable} parameter. The default
     * sort order is typically configured at the controller level.
     *
     * @param name optional substring filter on the courthouse name
     * @param courtType optional exact filter on the court type
     * @param pageable paging information including page number, size, and sort
     * @return a {@link Page} of {@link CourtLocationDto} results matching the criteria
     */
    Page<CourtLocationDto> searchCourtLocations(String name, String courtType, Pageable pageable);
}
