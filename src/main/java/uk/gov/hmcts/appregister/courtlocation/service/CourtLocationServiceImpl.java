package uk.gov.hmcts.appregister.courtlocation.service;

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
 * exceptions when required.
 *
 * <p>Responsibilities include:
 *
 * <ul>
 *   <li>Fetching all court locations as DTOs.
 *   <li>Looking up a specific court location by ID, throwing 404 if not found.
 *   <li>Searching for court locations with optional filters (name and court type) and paginated
 *       results.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CourtLocationServiceImpl implements CourtLocationService {

    // Repository providing persistence access to {@link CourtLocation} entities.
    private final CourtLocationRepository repository;

    // Mapper to convert entities into API-facing DTOs.
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
        CourtLocation courtLocation =
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
     * @param name optional case-insensitive substring filter on name
     * @param courtType optional exact match on court type
     * @param pageable pagination and sorting information
     * @return paginated page of {@link CourtLocationDto}
     */
    @Override
    public Page<CourtLocationDto> searchCourtLocations(
            String name, String courtType, Pageable pageable) {

        // Build a specification that combines optional filters.
        Specification<CourtLocation> spec =
                Specification.allOf(nameSpec(name), courtTypeSpec(courtType));

        // Fetch from DB and map entities to DTOs.
        return repository.findAll(spec, pageable).map(mapper::toReadDto);
    }

    /**
     * Build a case-insensitive {@code LIKE} specification for the name filter.
     *
     * @param name filter value, may be null/blank
     * @return {@link Specification} for the filter, or null if no filter should be applied
     */
    private Specification<CourtLocation> nameSpec(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Build an exact-match specification for court type.
     *
     * @param ct court type filter, may be null/blank
     * @return {@link Specification} for the filter, or null if no filter should be applied
     */
    private Specification<CourtLocation> courtTypeSpec(String ct) {
        if (ct == null || ct.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("courtType"), ct);
    }
}
