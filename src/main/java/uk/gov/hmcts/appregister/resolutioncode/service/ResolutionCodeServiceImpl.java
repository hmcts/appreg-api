package uk.gov.hmcts.appregister.resolutioncode.service;

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
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;
import uk.gov.hmcts.appregister.resolutioncode.repository.ResolutionCodeRepository;

/**
 * Concrete service for read-only operations on {@link ResolutionCode} data.
 *
 * <p>Primary responsibilities:
 *
 * <ul>
 *   <li>Coordinating persistence calls via {@link ResolutionCodeRepository}.
 *   <li>Mapping entities to API-facing DTOs via {@link ResolutionCodeMapper}.
 *   <li>Applying high-level business behavior (e.g., 404 on missing records).
 * </ul>
 *
 * <p><strong>Notes on mapping:</strong> The mapper returns {@code Optional<...>} to make
 * null-safety explicit. This service therefore unwraps Optionals and fails fast if an empty
 * Optional is returned for a non-null entity.
 */
@Service
@RequiredArgsConstructor
public class ResolutionCodeServiceImpl implements ResolutionCodeService {

    // Persistence gateway for {@link ResolutionCode} entities.
    private final ResolutionCodeRepository repository;

    // Converts between entities and DTOs.
    private final ResolutionCodeMapper mapper;

    /**
     * Fetch all resolution codes (unpaged).
     *
     * <p>Intended for internal/admin use. For user-facing list views use {@link #search} so that
     * pagination and filtering are applied consistently.
     *
     * @return list of {@link ResolutionCodeDto}; may be empty
     */
    @Override
    public List<ResolutionCodeDto> findAll() {
        // Prefer a deterministic order for callers that present this list (sorted by name/title).
        Iterable<ResolutionCode> all = repository.findAll(Sort.by("name").ascending());

        // Mapper returns Optional; unwrap and discard empties. If we ever get Optional.empty()
        // for a non-null entity, that indicates a mapping bug rather than a runtime condition.
        return StreamSupport.stream(all.spliterator(), false)
                .map(mapper::toReadDto) // Stream<Optional<ResolutionCodeDto>>
                .flatMap(Optional::stream) // Stream<ResolutionCodeDto>
                .toList();
    }

    /**
     * Fetch a single resolution code by its business code (unique string).
     *
     * @param code business identifier from {@code resolution_code}
     * @return the mapped {@link ResolutionCodeDto}
     * @throws ResponseStatusException 404 if not found
     */
    @Override
    public ResolutionCodeDto findByCode(String code) {
        // 404 if the business code doesn’t exist.
        ResolutionCode resultCode =
                repository
                        .findByResultCode(code)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "ResultCode not found"));

        // Mapper returns Optional; treat empty as illegal state for a non-null entity.
        return mapper.toReadDto(resultCode)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Mapper returned empty Optional for non-null entity"));
    }

    /**
     * Search result codes with optional filters and pagination.
     *
     * <p>Supported filters (all optional):
     *
     * <ul>
     *   <li>{@code code}: case-insensitive partial match
     *   <li>{@code title}: case-insensitive partial match
     *   <li>{@code startDateFrom}/{@code startDateTo}: inclusive bounds on validity start
     *   <li>{@code endDateFrom}/{@code endDateTo}: inclusive bounds on validity end
     * </ul>
     *
     * <p>Pagination/sorting is driven entirely by {@link Pageable} supplied by the controller
     * (e.g., default sort by title ASC).
     *
     * @return a {@link Page} of {@link ResolutionCodeListItemDto}
     */
    @Override
    public Page<ResolutionCodeListItemDto> search(
            String code,
            String title,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable) {
        // Helper that unwraps Optional from the mapper and fails loudly if empty.
        // Page#map requires a non-null mapping result for each element.
        java.util.function.Function<ResolutionCode, ResolutionCodeListItemDto> mapOrThrow =
                entity ->
                        mapper.toListItem(entity)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Mapper returned empty Optional for non-null entity"));

        // Delegate filtering to the repository’s @Query method; map entities to lightweight list
        // items.
        return repository
                .search(code, title, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable)
                .map(mapOrThrow);
    }
}
