package uk.gov.hmcts.appregister.resolutioncode.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.model.ResolutionCode;

/**
 * Service layer contract for interacting with {@link
 * ResolutionCode} data.
 *
 * <p>This interface defines operations available for reading and searching result codes, providing
 * an abstraction between controllers and the persistence layer. Implementations are responsible
 * for:
 *
 * <ul>
 *   <li>Fetching all result codes as full DTOs.
 *   <li>Looking up a single result code by its short code.
 *   <li>Searching for result codes with optional filters and pagination support.
 * </ul>
 *
 * <p>Controllers depend on this interface rather than directly on repositories. Implementations
 * will typically delegate to {@code ResultCodeRepository} and use {@code ResultCodeMapper} for
 * entity-to-DTO conversion.
 */
public interface ResolutionCodeService {

    /**
     * Fetches all result codes without filtering or pagination.
     *
     * <p>Intended for scenarios where a full list is needed, such as admin data exports or
     * initialisation tasks. For user-facing APIs, prefer the {@link #search} method which provides
     * pagination and filtering.
     *
     * @return list of all {@link ResolutionCodeDto} records; may be empty if no records exist
     */
    List<ResolutionCodeDto> findAll();

    /**
     * Finds a single result code by its business identifier (short code).
     *
     * @param code the short code string to search for (must match the {@code resolution_code}
     *     column)
     * @return the corresponding {@link ResolutionCodeDto}
     * @throws org.springframework.web.server.ResponseStatusException with status 404 (NOT_FOUND) if
     *     no result code exists for the given code
     */
    ResolutionCodeDto findByCode(String code);

    /**
     * Searches for result codes using optional filters and pagination.
     *
     * <p>Filters applied:
     *
     * <ul>
     *   <li>{@code code} – case-insensitive substring filter on the result code.
     *   <li>{@code title} – case-insensitive substring filter on the title.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range on {@code startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range on {@code endDate}.
     * </ul>
     *
     * <p><strong>Pagination:</strong> Provided via {@link Pageable}, which allows the caller to
     * specify page number, size, and sort order. Default sorting is typically by title ASC.
     *
     * @param code optional substring filter for the result code value
     * @param title optional substring filter for the result code title
     * @param startDateFrom optional lower bound (inclusive) for {@code startDate}
     * @param startDateTo optional upper bound (inclusive) for {@code startDate}
     * @param endDateFrom optional lower bound (inclusive) for {@code endDate}
     * @param endDateTo optional upper bound (inclusive) for {@code endDate}
     * @param pageable pagination and sorting information
     * @return a {@link Page} of {@link ResolutionCodeListItemDto} records matching the criteria; will
     *     include total count and pagination metadata
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
