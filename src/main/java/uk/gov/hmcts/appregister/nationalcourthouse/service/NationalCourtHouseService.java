package uk.gov.hmcts.appregister.nationalcourthouse.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;

/**
 * Read-only service contract for National Court House data.
 *
 * <p>This interface decouples controllers from persistence and mapping concerns. Implementations
 * typically compose repository queries (JPQL, Specifications, or derived queries) and transform
 * entities into API-facing DTOs via {@link NationalCourtHouseMapper}.
 *
 * <p><strong>Repository expectations:</strong> Implementations will use {@link
 * NationalCourtHouseRepository}, which extends {@code PagingAndSortingRepository} for
 * pagination/sorting and {@code JpaSpecificationExecutor} for dynamic filtering.
 *
 * <p><strong>Pagination model:</strong> Methods that accept a {@link Pageable} expect 0-based
 * paging (Spring Data convention). Controllers may expose 1-based parameters and adapt them.
 */
public interface NationalCourtHouseService {

    /**
     * Fetch all court locations without pagination or filtering.
     *
     * <p>Intended for internal/admin use only; most client use cases should prefer the paginated
     * {@link #search(String, String, LocalDate, LocalDate, LocalDate, LocalDate, Pageable)} to
     * avoid large payloads.
     *
     * @return a list of {@link NationalCourtHouseDto}; may be empty
     */
    List<NationalCourtHouseDto> findAll();

    /**
     * Find a single court location by its identifier.
     *
     * <p>Implementations should translate a missing entity to a {@code 404} (e.g., by throwing
     * {@code ResponseStatusException(HttpStatus.NOT_FOUND)}).
     *
     * @param id unique identifier of the court location
     * @return the matching {@link NationalCourtHouseDto}
     * @throws org.springframework.web.server.ResponseStatusException with status 404 if not found
     */
    NationalCourtHouseDto findById(Long id);

    /**
     * Search for court locations using optional filters and pagination.
     *
     * <p><strong>Filters (all optional):</strong>
     *
     * <ul>
     *   <li>{@code name} – case-insensitive substring match.
     *   <li>{@code courtType} – exact match.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range on {@code startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range on {@code endDate}.
     *       Implementations commonly treat {@code endDate = NULL} as “ongoing” and include those
     *       when {@code endDateFrom} is supplied.
     * </ul>
     *
     * <p><strong>Paging/Sorting:</strong> Provided via {@link Pageable}. Controllers typically
     * supply a default sort (e.g., {@code Sort.by("name").ascending()}). Results should already be
     * mapped to DTOs.
     *
     * @param name case-insensitive substring filter on name (nullable = no filter)
     * @param courtType exact match on court type (nullable = no filter)
     * @param startDateFrom lower bound for {@code startDate} (inclusive), or {@code null}
     * @param startDateTo upper bound for {@code startDate} (inclusive), or {@code null}
     * @param endDateFrom lower bound for {@code endDate} (inclusive), or {@code null}
     * @param endDateTo upper bound for {@code endDate} (inclusive), or {@code null}
     * @param pageable page number/size/sort (0-based page index)
     * @return a {@link Page} of {@link NationalCourtHouseDto} matching the criteria
     */
    Page<NationalCourtHouseDto> search(
            String name,
            String courtType,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable);
}
