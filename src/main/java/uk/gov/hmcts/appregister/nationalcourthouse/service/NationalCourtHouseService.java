package uk.gov.hmcts.appregister.nationalcourthouse.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;
import uk.gov.hmcts.appregister.nationalcourthouse.repository.NationalCourtHouseRepository;

/**
 * Service layer contract for interacting with Court Location data.
 *
 * <p>This interface defines the operations available for reading Court Location records, decoupling
 * controllers from the repository layer. Implementations are responsible for applying business
 * rules, mapping JPA entities to DTOs, and delegating persistence operations to a repository.
 *
 * <p><b>Usage pattern:</b>
 *
 * <ul>
 *   <li>Controllers depend on this interface rather than directly on repositories.
 *   <li>Implementations typically use {@link
 *       NationalCourtHouseRepository} and {@link
 *       NationalCourtHouseMapper}.
 * </ul>
 */
public interface NationalCourtHouseService {

    /**
     * Fetch all court locations without pagination or filtering.
     *
     * @return a complete list of {@link NationalCourtHouseDto} objects; may be empty if no court
     *     locations exist
     */
    List<NationalCourtHouseDto> findAll();

    /**
     * Find a single court location by its identifier.
     *
     * @param id the unique identifier of the court location
     * @return the corresponding {@link NationalCourtHouseDto}
     * @throws org.springframework.web.server.ResponseStatusException with status 404 (NOT_FOUND) if
     *     no court location exists for the given id
     */
    NationalCourtHouseDto findById(Long id);

    /**
     * Search for court locations using optional filters and pagination.
     *
     * <p><b>Filters:</b>
     *
     * <ul>
     *   <li>{@code name} – optional case-insensitive substring filter on the courthouse name.
     *   <li>{@code courtType} – optional exact match on the {@code courtType} field.
     *   <li>{@code startDateFrom}/{@code startDateTo} – optional inclusive range on {@code
     *       startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – optional inclusive range on {@code endDate}.
     *       Implementations should define semantics for {@code NULL endDate} (e.g., treat as
     *       “ongoing” when applying {@code endDateFrom}).
     * </ul>
     *
     * <p><b>Paging/Sorting:</b> Provided via the {@link Pageable} argument. Controllers typically
     * supply a default sort (e.g., {@code Sort.by("name").ascending()}).
     *
     * @param name optional substring filter on the courthouse name; case-insensitive
     * @param courtType optional exact match on the court type
     * @param startDateFrom optional lower bound for {@code startDate} (inclusive)
     * @param startDateTo optional upper bound for {@code startDate} (inclusive)
     * @param endDateFrom optional lower bound for {@code endDate} (inclusive)
     * @param endDateTo optional upper bound for {@code endDate} (inclusive)
     * @param pageable paging information including page number, size, and sort
     * @return a {@link Page} of {@link NationalCourtHouseDto} results matching the criteria
     */
    Page<NationalCourtHouseDto> searchCourtLocations(
            String name,
            String courtType,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable); // controller provides 0-based paging + sort; service composes
    // filters
}
