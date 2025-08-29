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
 * REST controller exposing read-only endpoints for Court Locations.
 *
 * <p>Features:
 *
 * <ul>
 *   <li><b>Paginated listing</b> with optional filters (name, courtType, start/end date ranges).
 *   <li><b>Fetch-by-id</b> endpoint for a single record.
 * </ul>
 *
 * <p><b>Pagination model:</b> The public API is 1-based (i.e., {@code page=1} is the first page),
 * while Spring Data is 0-based. The controller converts to 0-based internally.
 *
 * <p><b>Validation:</b> Returns {@code 400 Bad Request} when:
 *
 * <ul>
 *   <li>{@code page < 1}
 *   <li>{@code pageSize < 1} or {@code pageSize > MAX_PAGE_SIZE}
 *   <li>{@code startDateFrom > startDateTo}
 *   <li>{@code endDateFrom > endDateTo}
 * </ul>
 *
 * <p>All list results are sorted by {@code name ASC} for deterministic UI ordering.
 */
@RestController
@RequestMapping("/national-court-house")
@RequiredArgsConstructor
public class NationalCourtHouseController {

    /** Application service used to query court locations. */
    private final NationalCourtHouseService service;

    private final DateRangeValidator dateRangeValidator;

    /**
     * Returns a paginated list of court locations with optional filters.
     *
     * <p><b>Filters:</b>
     *
     * <ul>
     *   <li>{@code name} – case-insensitive substring match on the court location name.
     *   <li>{@code courtType} – exact match.
     *   <li>{@code startDateFrom}/{@code startDateTo} – constrain the {@code start_date}
     *       (inclusive).
     *   <li>{@code endDateFrom}/{@code endDateTo} – constrain the {@code end_date} (inclusive).
     * </ul>
     *
     * <p><b>Date format:</b> All dates are ISO-8601 ({@code yyyy-MM-dd}). When a given end of a
     * range is omitted, only the other bound is applied. If both ends are supplied, the controller
     * validates that {@code from <= to}.
     *
     * <p><b>Sorting:</b> Fixed to {@code name ASC}.
     *
     * <p><b>Responses:</b>
     *
     * <ul>
     *   <li>{@code 200 OK} with {@link NationalCourtHouseDto} on success.
     *   <li>{@code 400 Bad Request} when input validation fails (see class-level notes).
     * </ul>
     *
     * @param name optional case-insensitive substring filter on court location name
     * @param courtType optional exact-match court type filter
     * @param startDateFrom optional lower bound for {@code start_date} (inclusive)
     * @param startDateTo optional upper bound for {@code start_date} (inclusive)
     * @param endDateFrom optional lower bound for {@code end_date} (inclusive)
     * @param endDateTo optional upper bound for {@code end_date} (inclusive)
     * @return {@link ResponseEntity} containing {@link NationalCourtHouseDto} or {@code
     *     400}
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
        @RequestParam(required = false) java.time.LocalDate startDateFrom,
        @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) java.time.LocalDate startDateTo,
        @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) java.time.LocalDate endDateFrom,
        @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) java.time.LocalDate endDateTo,
        @org.springframework.data.web.PageableDefault(sort = "name",
            direction = org.springframework.data.domain.Sort.Direction.ASC)
        Pageable pageable
    ) {
        dateRangeValidator.validate(startDateFrom, startDateTo, endDateFrom, endDateTo);

        Page<NationalCourtHouseDto> page = service.search(
            name, courtType, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable
        );

        return ResponseEntity.ok(page);
    }

    /**
     * Fetches a single court location by its identifier.
     *
     * <p>On missing entity the service is expected to throw a {@code
     * ResponseStatusException(HttpStatus.NOT_FOUND)} which Spring maps to a {@code 404}.
     *
     * @param id the identifier of the court location
     * @return {@link ResponseEntity} with a {@link NationalCourtHouseDto} for {@code 200 OK}
     * @see NationalCourtHouseService#findById(Long)
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
        // Delegate to service; exceptions are propagated and translated by Spring.
        return ResponseEntity.ok(service.findById(id));
    }
}
