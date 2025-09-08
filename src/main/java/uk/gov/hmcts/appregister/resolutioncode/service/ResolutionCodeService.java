package uk.gov.hmcts.appregister.resolutioncode.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

/**
 * Service-layer contract for interacting with {@link ResolutionCode} data.
 *
 * <p>This interface defines read/search operations and decouples controllers from persistence.
 * Implementations typically delegate to a Spring Data repository and use a mapper for entity-to-DTO
 * conversion.
 *
 * <p><strong>Typical responsibilities:</strong>
 *
 * <ul>
 *   <li>Fetch all result codes as full DTOs (administrative use).
 *   <li>Lookup of a single result code by its business code.
 *   <li>Paginated, filterable search returning lightweight list items for UI lists.
 * </ul>
 */
public interface ResolutionCodeService {

    /**
     * Returns every result code without filtering or pagination.
     *
     * <p>Prefer {@link #search(String, String, LocalDate, LocalDate, LocalDate, LocalDate,
     * Pageable)} for user-facing workflows, as it provides paging and filtering.
     *
     * @return a complete list of {@link ResolutionCodeDto}; may be empty
     */
    List<ResolutionCodeDto> findAll();

    /**
     * Find a single resolution code by its identifier.
     *
     * <p>Implementations should translate a missing entity to a {@code 404} (e.g., by throwing
     * {@code ResponseStatusException(HttpStatus.NOT_FOUND)}).
     *
     * @param id unique identifier of the resolution code
     * @return the matching {@link ResolutionCodeDto}
     * @throws org.springframework.web.server.ResponseStatusException with status 404 if not found
     */
    ResolutionCodeDto findById(Long id);

    /**
     * Searches result codes with optional filters and pagination.
     *
     * <p><strong>Filter semantics:</strong>
     *
     * <ul>
     *   <li><b>code</b>: case-insensitive partial match (ILIKE semantics).
     *   <li><b>title</b>: case-insensitive partial match.
     *   <li><b>startDateFrom / startDateTo</b>: inclusive bounds on {@code startDate}.
     *   <li><b>endDateFrom</b>: includes records where {@code endDate} is <em>null</em> (treated as
     *       ongoing) <strong>or</strong> {@code endDate >= endDateFrom}.
     *   <li><b>endDateTo</b>: inclusive upper bound on {@code endDate}; {@code null} end dates do
     *       not match this predicate.
     *   <li>Any {@code null} parameter disables that specific filter.
     * </ul>
     *
     * <p><strong>Pagination & sorting:</strong> Provided via {@link Pageable}. Controllers commonly
     * default to {@code Sort.by("title").ascending()} for deterministic UI ordering.
     *
     * @param code optional case-insensitive partial code filter
     * @param title optional case-insensitive partial title filter
     * @param startDateFrom optional lower bound (inclusive) for {@code startDate}
     * @param startDateTo optional upper bound (inclusive) for {@code startDate}
     * @param endDateFrom optional lower bound (inclusive) for {@code endDate}; also matches ongoing
     *     ({@code null}) end dates
     * @param endDateTo optional upper bound (inclusive) for {@code endDate}
     * @param pageable page/size/sort
     * @return a {@link Page} of {@link ResolutionCodeListItemDto} with results and paging metadata
     */
    Page<ResolutionCodeListItemDto> search(
            String code,
            String title,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo,
            Pageable pageable);
}
