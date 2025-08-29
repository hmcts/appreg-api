package uk.gov.hmcts.appregister.nationalcourthouse.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;
import uk.gov.hmcts.appregister.nationalcourthouse.repository.NationalCourtHouseRepository;

/**
 * Service implementation for interacting with {@link NationalCourtHouse} data.
 *
 * <p>This class acts as the bridge between controllers and the repository layer: it applies
 * filtering, performs mapping into {@link NationalCourtHouseDto}, and raises appropriate HTTP-level
 * exceptions when required.
 *
 * <p>Responsibilities include:
 *
 * <ul>
 *   <li>Fetching all court locations as DTOs.
 *   <li>Looking up a specific court location by ID, throwing 404 if not found.
 *   <li>Searching for court locations with optional filters (name, court type, start/end dates) and
 *       paginated results.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NationalCourtHouseServiceImpl implements NationalCourtHouseService {

    // Repository providing persistence access to {@link CourtLocation} entities.
    private final NationalCourtHouseRepository repository;

    // Mapper to convert entities into API-facing DTOs.
    private final NationalCourtHouseMapper mapper;

    /**
     * Fetch all court locations without filtering or pagination.
     *
     * @return list of {@link NationalCourtHouseDto} (possibly empty if no records exist)
     */
    @Override
    public List<NationalCourtHouseDto> findAll() {
        final List<NationalCourtHouse> courtLocations = repository.findAll();
        // Map each JPA entity into a DTO for external use.
        return courtLocations.stream().map(mapper::toReadDto).toList();
    }

    /**
     * Find a single court location by ID.
     *
     * @param id the ID of the court location
     * @return the corresponding {@link NationalCourtHouseDto}
     * @throws ResponseStatusException with {@code 404 NOT_FOUND} if not present
     */
    @Override
    public NationalCourtHouseDto findById(Long id) {
        NationalCourtHouse courtLocation =
                repository
                        .findById(id)
                        // Translate "not found" into a 404 for the REST API layer.
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "CourtLocation not found"));
        return mapper.toReadDto(courtLocation);
    }

    /**
     * Search court locations with optional filters and pagination.
     *
     * <p>Filters applied:
     *
     * <ul>
     *   <li>{@code name} – case-insensitive substring filter.
     *   <li>{@code courtType} – exact match.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range filter on {@code
     *       startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range filter on {@code endDate}.
     * </ul>
     *
     * <p>Pagination and sorting are handled by {@link Pageable}.
     *
     * @return a {@link Page} of {@link NationalCourtHouseDto} matching the criteria
     */
    @Override
    public Page<NationalCourtHouseDto> searchCourtLocations(
            String name,
            String courtType,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable) {

        // Combine all optional specifications into one
        Specification<NationalCourtHouse> spec =
                Specification.allOf(
                        nameSpec(name),
                        courtTypeSpec(courtType),
                        startDateFromSpec(startDateFrom),
                        startDateToSpec(startDateTo),
                        endDateFromSpec(endDateFrom),
                        endDateToSpec(endDateTo));

        return repository.findAll(spec, pageable).map(mapper::toReadDto);
    }

    /** Build specification: start_date >= from. */
    private Specification<NationalCourtHouse> startDateFromSpec(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    /** Build specification: start_date <= to. */
    private Specification<NationalCourtHouse> startDateToSpec(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    /**
     * Build specification: end_date >= from OR end_date IS NULL.
     *
     * <p>Treats {@code null end_date} as "ongoing" and therefore included in queries constrained by
     * a lower bound.
     */
    private Specification<NationalCourtHouse> endDateFromSpec(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, q, cb) ->
                cb.or(
                        cb.isNull(root.get("endDate")),
                        cb.greaterThanOrEqualTo(root.get("endDate"), from));
    }

    /** Build specification: end_date <= to (NULLs excluded by default). */
    private Specification<NationalCourtHouse> endDateToSpec(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }

    /**
     * Build a case-insensitive {@code LIKE} specification for the name filter.
     *
     * @param name filter value, may be null/blank
     * @return {@link Specification} for the filter, or null if not applied
     */
    private Specification<NationalCourtHouse> nameSpec(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Build an exact-match specification for court type.
     *
     * @param ct court type filter, may be null/blank
     * @return {@link Specification} for the filter, or null if not applied
     */
    private Specification<NationalCourtHouse> courtTypeSpec(String ct) {
        if (ct == null || ct.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("courtType"), ct);
    }
}
