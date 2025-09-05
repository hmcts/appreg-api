package uk.gov.hmcts.appregister.nationalcourthouse.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.repository.NationalCourtHouseRepository;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;

/**
 * Service implementation for interacting with {@link NationalCourtHouse} data.
 *
 * <p>This service sits between the web layer and the persistence layer and is responsible for:
 *
 * <ul>
 *   <li>Applying read filters (name, type, effective dates) and pagination.
 *   <li>Mapping JPA entities into API-facing DTOs via {@link NationalCourtHouseMapper}.
 *   <li>Translating not-found scenarios into HTTP-friendly exceptions for the controller.
 * </ul>
 *
 * <p><strong>Mapper contract:</strong> {@link
 * NationalCourtHouseMapper#toReadDto(NationalCourtHouse)} returns an {@code
 * Optional<NationalCourtHouseDto>} rather than a bare DTO. This allows the mapper to decline
 * mapping (e.g. if an entity is missing required data). Callers in this service unwrap the optional
 * either by filtering empties (in {@link #findAll()}) or by failing fast (in {@link #search(String,
 * String, LocalDate, LocalDate, LocalDate, LocalDate, Pageable)}).
 */
@Service
@RequiredArgsConstructor
public class NationalCourtHouseServiceImpl implements NationalCourtHouseService {

    /** Persistence access to court house entities (paging & sorting supported). */
    private final NationalCourtHouseRepository repository;

    /** Converts entities to DTOs without leaking JPA concerns to API clients. */
    private final NationalCourtHouseMapper mapper;

    /**
     * Fetch all court locations, sorted by name ascending.
     *
     * <p>Because the mapper returns {@code Optional<DTO>}, the stream flattens empty optionals (if
     * any) to avoid {@code null} elements in the result list.
     *
     * @return list of {@link NationalCourtHouseDto}; possibly empty
     */
    @Override
    public List<NationalCourtHouseDto> findAll() {
        // PagingAndSortingRepository gives us sorted iteration without having to build a Page.
        Iterable<NationalCourtHouse> all = repository.findAll(Sort.by("name").ascending());

        return StreamSupport.stream(all.spliterator(), false)
                .map(mapper::toReadDto) // Stream<Optional<Dto>>
                .flatMap(Optional::stream) // Stream<Dto> — drop any empty optionals
                .toList();
    }

    /**
     * Retrieve one court location by id; throws 404 if it does not exist.
     *
     * <p>We unwrap in two stages:
     *
     * <ol>
     *   <li>Repository {@code findById} → {@code Optional<entity>}, 404 if missing.
     *   <li>Mapper {@code toReadDto(entity)} → {@code Optional<dto>}, 404 if mapping declined.
     * </ol>
     *
     * @param id entity identifier
     * @return mapped DTO
     * @throws ResponseStatusException 404 when not found (or mapping produces empty)
     */
    @Override
    public NationalCourtHouseDto findById(Long id) {
        return repository
                .findById(id)
                .flatMap(mapper::toReadDto) // Optional<NationalCourtHouseDto>
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "CourtLocation not found"));
    }

    /**
     * Search court locations with optional filters and pagination.
     *
     * <p><strong>Filters</strong> (all optional):
     *
     * <ul>
     *   <li>{@code name}: case-insensitive substring match
     *   <li>{@code courtType}: exact match
     *   <li>{@code startDateFrom}/{@code startDateTo}: inclusive range on {@code startDate}
     *   <li>{@code endDateFrom}/{@code endDateTo}: inclusive range on {@code endDate} (repository
     *       query typically treats {@code null endDate} as "ongoing")
     * </ul>
     *
     * <p>Pagination and sorting are supplied by {@link Pageable} from the controller. The
     * repository returns a {@link Page} which we map element-wise to DTOs while preserving page
     * metadata.
     *
     * <p>If the mapper ever returns an empty {@code Optional} for a non-null entity, we fail fast
     * with an {@link IllegalStateException} so the issue is visible during development and testing.
     *
     * @return a {@link Page} of {@link NationalCourtHouseDto} matching the criteria
     */
    @Override
    public Page<NationalCourtHouseDto> search(
            String name,
            String courtType,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable) {

        // Unwrap the Optional from the mapper; fail fast if mapping was declined for a real entity.
        java.util.function.Function<NationalCourtHouse, NationalCourtHouseDto> mapOrThrow =
                entity ->
                        mapper.toReadDto(entity)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Mapper returned empty Optional for non-null entity"));

        // Delegate filtering & paging to the repository, then map each entity to its DTO.
        return repository
                .search(
                        name,
                        courtType,
                        startDateFrom,
                        startDateTo,
                        endDateFrom,
                        endDateTo,
                        pageable)
                .map(mapOrThrow);
    }
}
