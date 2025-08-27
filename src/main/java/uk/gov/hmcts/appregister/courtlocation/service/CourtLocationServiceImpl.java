package uk.gov.hmcts.appregister.courtlocation.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;
import uk.gov.hmcts.appregister.courtlocation.mapper.CourtLocationMapper;
import uk.gov.hmcts.appregister.courtlocation.model.CourtLocation;
import uk.gov.hmcts.appregister.courtlocation.repository.CourtLocationRepository;

/**
 * Service implementation for interacting with {@link CourtLocation} data.
 *
 * <p>This class acts as the bridge between controllers and the repository layer: it applies
 * filtering, performs mapping into {@link CourtLocationDto}, and raises appropriate HTTP-level
 * exceptions when required.</p>
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Fetching all court locations as DTOs.</li>
 *   <li>Looking up a specific court location by ID, throwing 404 if not found.</li>
 *   <li>Searching for court locations with optional filters (name, court type, start/end dates)
 *       and paginated results.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CourtLocationServiceImpl implements CourtLocationService {

    /** Repository providing persistence access to {@link CourtLocation} entities. */
    private final CourtLocationRepository repository;

    /** Mapper to convert entities into API-facing DTOs. */
    private final CourtLocationMapper mapper;

    /**
     * Fetch all court locations without filtering or pagination.
     *
     * @return list of {@link CourtLocationDto} (possibly empty if no records exist)
     */
    @Override
    public List<CourtLocationDto> findAll() {
        final List<CourtLocation> courtLocations = repository.findAll();
        // Map each JPA entity into a DTO for external use.
        return courtLocations.stream().map(mapper::toReadDto).toList();
    }

    /**
     * Find a single court location by ID.
     *
     * @param id the ID of the court location
     * @return the corresponding {@link CourtLocationDto}
     * @throws ResponseStatusException with {@code 404 NOT_FOUND} if not present
     */
    @Override
    public CourtLocationDto findById(Long id) {
        CourtLocation courtLocation = repository
            .findById(id)
            // Translate "not found" into a 404 for the REST API layer.
            .orElseThrow(() ->
                             new ResponseStatusException(HttpStatus.NOT_FOUND, "CourtLocation not found"));
        return mapper.toReadDto(courtLocation);
    }

    /**
     * Search court locations with optional filters and pagination.
     *
     * <p>Filters applied:
     * <ul>
     *   <li>{@code name} – case-insensitive substring filter.</li>
     *   <li>{@code courtType} – exact match.</li>
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range filter on {@code startDate}.</li>
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range filter on {@code endDate}.</li>
     * </ul>
     * Pagination and sorting are handled by {@link Pageable}.
     *
     * @return a {@link Page} of {@link CourtLocationDto} matching the criteria
     */
    @Override
    public Page<CourtLocationDto> searchCourtLocations(
        String name,
        String courtType,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        LocalDate endDateFrom,
        LocalDate endDateTo,
        Pageable pageable) {

        // Combine all optional specifications into one
        Specification<CourtLocation> spec = Specification.allOf(
            nameSpec(name),
            courtTypeSpec(courtType),
            startDateFromSpec(startDateFrom),
            startDateToSpec(startDateTo),
            endDateFromSpec(endDateFrom),
            endDateToSpec(endDateTo)
        );

        return repository.findAll(spec, pageable).map(mapper::toReadDto);
    }

    /** Build specification: start_date >= from */
    private Specification<CourtLocation> startDateFromSpec(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    /** Build specification: start_date <= to */
    private Specification<CourtLocation> startDateToSpec(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    /**
     * Build specification: end_date >= from OR end_date IS NULL.
     * <p>Treats {@code null end_date} as "ongoing" and therefore included
     * in queries constrained by a lower bound.</p>
     */
    private Specification<CourtLocation> endDateFromSpec(LocalDate from) {
        if (from == null) return null;
        return (root, q, cb) -> cb.or(
            cb.isNull(root.get("endDate")),
            cb.greaterThanOrEqualTo(root.get("endDate"), from)
        );
    }

    /** Build specification: end_date <= to (NULLs excluded by default). */
    private Specification<CourtLocation> endDateToSpec(LocalDate to) {
        if (to == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }

    /**
     * Build a case-insensitive {@code LIKE} specification for the name filter.
     *
     * @param name filter value, may be null/blank
     * @return {@link Specification} for the filter, or null if not applied
     */
    private Specification<CourtLocation> nameSpec(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, q, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Build an exact-match specification for court type.
     *
     * @param ct court type filter, may be null/blank
     * @return {@link Specification} for the filter, or null if not applied
     */
    private Specification<CourtLocation> courtTypeSpec(String ct) {
        if (ct == null || ct.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("courtType"), ct);
    }
}
