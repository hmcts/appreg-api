package uk.gov.hmcts.appregister.resolutioncode.service;

import static java.util.Objects.nonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;
import uk.gov.hmcts.appregister.resolutioncode.repository.ResolutionCodeRepository;

/**
 * Service implementation of {@link ResolutionCodeService}.
 *
 * <p>This class acts as the bridge between controllers and the repository layer: it applies
 * filtering logic, performs entity-to-DTO mapping, and raises appropriate HTTP-level exceptions
 * when required.
 *
 * <p><strong>Responsibilities:</strong>
 *
 * <ul>
 *   <li>Fetch all result codes as full DTOs.
 *   <li>Find a single result code by its business identifier (code).
 *   <li>Search result codes with optional filters and pagination using {@link Specification}.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ResolutionCodeServiceImpl implements ResolutionCodeService {

    // Spring Data repository providing persistence access to {@link ResultCode} entities.
    private final ResolutionCodeRepository repository;

    // Mapper for converting between {@link ResultCode} entities and API-facing DTOs.
    private final ResolutionCodeMapper mapper;

    /**
     * Fetches all result codes without pagination or filtering.
     *
     * @return a list of {@link ResolutionCodeDto}; may be empty if no records exist
     */
    @Override
    public List<ResolutionCodeDto> findAll() {
        Iterable<ResolutionCode> all = repository.findAll(Sort.by("name").ascending());
        return StreamSupport.stream(all.spliterator(), false)
            .map(mapper::toReadDto)
            .toList();
    }

    /**
     * Finds a result code by its business identifier (code).
     *
     * @param code the short code string to search for
     * @return the corresponding {@link ResolutionCodeDto}
     * @throws ResponseStatusException with status 404 (NOT_FOUND) if not present
     */
    @Override
    public ResolutionCodeDto findByCode(String code) {
        final ResolutionCode resultCode =
                repository
                        .findByResultCode(code)
                        // Translate missing record into an HTTP 404 for API consistency.
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "ResultCode not found"));

        return mapper.toReadDto(resultCode);
    }

    /**
     * Searches for result codes with optional filters and pagination.
     *
     * <p>Filters applied:
     *
     * <ul>
     *   <li>{@code code} – case-insensitive substring filter.
     *   <li>{@code title} – case-insensitive substring filter.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range on start date.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range on end date.
     * </ul>
     *
     * @param code optional substring filter for code
     * @param title optional substring filter for title
     * @param startDateFrom optional lower bound for start date
     * @param startDateTo optional upper bound for start date
     * @param endDateFrom optional lower bound for end date
     * @param endDateTo optional upper bound for end date
     * @param pageable pagination and sorting information
     * @return a page of {@link ResolutionCodeListItemDto} records matching the criteria
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

        // Compose a combined specification of all filters.
        Specification<ResolutionCode> spec =
                Specification.allOf(
                        codeLikeSpec(code),
                        titleLikeSpec(title),
                        startDateFromSpec(startDateFrom),
                        startDateToSpec(startDateTo),
                        endDateFromSpec(endDateFrom),
                        endDateToSpec(endDateTo));

        // Execute repository search and map each entity to a lightweight list item DTO.
        return repository.findAll(spec, pageable).map(mapper::toListItem);
    }

    // ---------------- Private Specification Builders ----------------

    // Builds a case-insensitive LIKE specification for code (ILIKE semantics).
    private Specification<ResolutionCode> codeLikeSpec(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        final String needle = "%" + code.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("resultCode")), needle);
    }

    // Builds a case-insensitive LIKE specification for title.
    private Specification<ResolutionCode> titleLikeSpec(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        final String needle = "%" + title.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("title")), needle);
    }

    // Builds a specification enforcing {@code startDate >= from}.
    private Specification<ResolutionCode> startDateFromSpec(LocalDate from) {
        return nonNull(from)
                ? (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startDate"), from)
                : null;
    }

    // Builds a specification enforcing {@code startDate <= to}.
    private Specification<ResolutionCode> startDateToSpec(LocalDate to) {
        return nonNull(to) ? (r, q, cb) -> cb.lessThanOrEqualTo(r.get("startDate"), to) : null;
    }

    // Builds a specification enforcing {@code endDate >= from} or {@code endDate IS NULL}.
    private Specification<ResolutionCode> endDateFromSpec(LocalDate from) {
        return nonNull(from)
                ? (r, q, cb) ->
                        cb.or(
                                cb.isNull(r.get("endDate")),
                                cb.greaterThanOrEqualTo(r.get("endDate"), from))
                : null;
    }

    // Builds a specification enforcing {@code endDate <= to} (nulls excluded).
    private Specification<ResolutionCode> endDateToSpec(LocalDate to) {
        return nonNull(to) ? (r, q, cb) -> cb.lessThanOrEqualTo(r.get("endDate"), to) : null;
    }
}
