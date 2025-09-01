package uk.gov.hmcts.appregister.nationalcourthouse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.service.NationalCourtHouseService;
import uk.gov.hmcts.appregister.shared.validation.DateRangeValidator;

/**
 * Read-only REST controller for National Court Houses.
 *
 * <p><strong>What this exposes</strong>
 *
 * <ul>
 *   <li>A paginated, filterable listing endpoint that returns a Spring {@link Page} of {@link
 *       NationalCourtHouseDto} (no custom wrapper object).
 *   <li>A fetch-by-ID endpoint.
 * </ul>
 *
 * <p><strong>Pagination &amp; sorting</strong>
 *
 * <ul>
 *   <li>Pagination is handled by Spring via {@link Pageable} query parameters (e.g. {@code page},
 *       {@code size}, {@code sort}).
 *   <li>A default sort of {@code name,ASC} is provided via {@code @PageableDefault}, and callers
 *       may override it using the standard {@code sort} query parameter.
 *   <li>Note: by Spring convention, {@code page} is <em>zero-based</em> (i.e., {@code page=0} is
 *       the first page).
 * </ul>
 *
 * <p><strong>Filtering</strong>
 *
 * <ul>
 *   <li>{@code name} – case-insensitive substring match.
 *   <li>{@code courtType} – exact match.
 *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range filter on start date.
 *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range filter on end date.
 * </ul>
 *
 * <p><strong>Validation</strong>
 *
 * <ul>
 *   <li>Date-range validation (ensuring {@code from &le; to} when both bounds provided) is
 *       delegated to {@link DateRangeValidator}. Invalid ranges result in {@code 400 Bad Request}.
 * </ul>
 */
@RestController
@RequestMapping("/national-court-house")
@RequiredArgsConstructor
public class NationalCourtHouseController {

    /** Application service used to retrieve National Court House data. */
    private final NationalCourtHouseService service;

    /** Shared validator for simple date range checks (throws 400 on invalid ranges). */
    private final DateRangeValidator dateRangeValidator;

    /**
     * List National Court Houses with optional filters and pageable response.
     *
     * <p>Returns a Spring {@link Page} of {@link NationalCourtHouseDto}. Clients control pagination
     * and sorting using standard Spring Data parameters (e.g., {@code
     * ?page=0&size=10&sort=name,asc}).
     *
     * @param name optional case-insensitive substring filter on the court house name
     * @param courtType optional exact-match filter on the court type
     * @param startDateFrom optional lower bound for start date (inclusive)
     * @param startDateTo optional upper bound for start date (inclusive)
     * @param endDateFrom optional lower bound for end date (inclusive)
     * @param endDateTo optional upper bound for end date (inclusive)
     * @param pageable standard Spring pageable (zero-based page index), defaulting to {@code
     *     name,ASC}
     * @return {@code 200 OK} with a {@link Page} of results; {@code 400 Bad Request} if date ranges
     *     are invalid
     */
    @Operation(
            summary = "Get court locations (paginated, filterable)",
            operationId = "getCourtLocations")
    @ApiResponse(
            responseCode = "200",
            description = "List of court locations retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<NationalCourtHouseDto>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String courtType,
            @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false)
                    java.time.LocalDate startDateFrom,
            @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false)
                    java.time.LocalDate startDateTo,
            @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false)
                    java.time.LocalDate endDateFrom,
            @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false)
                    java.time.LocalDate endDateTo,
            @org.springframework.data.web.PageableDefault(
                            sort = "name",
                            direction = org.springframework.data.domain.Sort.Direction.ASC)
                    Pageable pageable) {
        // Delegate date-range validity checks; throws ResponseStatusException(400) on failure.
        dateRangeValidator.validate(startDateFrom, startDateTo, endDateFrom, endDateTo);

        // Delegate to service: applies specs/filters and returns a Page<DTO>.
        Page<NationalCourtHouseDto> page =
                service.search(
                        name,
                        courtType,
                        startDateFrom,
                        startDateTo,
                        endDateFrom,
                        endDateTo,
                        pageable);

        // Return the Page directly; Spring serialises content + pagination metadata.
        return ResponseEntity.ok(page);
    }

    /**
     * Retrieve a single National Court House by its ID.
     *
     * <p>If the ID does not exist, the service raises a {@code ResponseStatusException(404)} which
     * Spring maps to {@code 404 Not Found}.
     *
     * @param id database identifier of the court house
     * @return {@code 200 OK} with the court house DTO; {@code 404 Not Found} if missing
     */
    @Operation(
            summary = "Get a specific court location by ID",
            operationId = "getCourtLocationById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Court location found"),
        @ApiResponse(responseCode = "404", description = "Court location not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NationalCourtHouseDto> getById(@PathVariable Long id) {
        // Service throws 404 if not found; we simply return 200 with the DTO on success.
        return ResponseEntity.ok(service.findById(id));
    }
}
