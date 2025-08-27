package uk.gov.hmcts.appregister.courtlocation.controller;

import static java.util.Objects.requireNonNullElse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationDto;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtLocationPageResponse;
import uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService;

/**
 * REST controller exposing read-only endpoints for Court Locations.
 *
 * <p>Features:
 * <ul>
 *   <li><b>Paginated listing</b> with optional filters (name, courtType, start/end date ranges).</li>
 *   <li><b>Fetch-by-id</b> endpoint for a single record.</li>
 * </ul>
 *
 * <p><b>Pagination model:</b> The public API is 1-based (i.e., {@code page=1} is the first page),
 * while Spring Data is 0-based. The controller converts to 0-based internally.
 *
 * <p><b>Validation:</b> Returns {@code 400 Bad Request} when:
 * <ul>
 *   <li>{@code page < 1}</li>
 *   <li>{@code pageSize < 1} or {@code pageSize > MAX_PAGE_SIZE}</li>
 *   <li>{@code startDateFrom > startDateTo}</li>
 *   <li>{@code endDateFrom > endDateTo}</li>
 * </ul>
 *
 * <p>All list results are sorted by {@code name ASC} for deterministic UI ordering.
 */
@RestController
@RequestMapping("/court-locations")
@RequiredArgsConstructor
public class CourtLocationController {

    /**
     * Default page number for public API (1-based).
     */
    private static final int DEFAULT_PAGE = 1;

    /**
     * Default page size when not specified by the client.
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Upper bound on page size to protect the service from overly large requests.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Application service used to query court locations.
     */
    private final CourtLocationService service;

    /**
     * Returns a paginated list of court locations with optional filters.
     *
     * <p><b>Filters:</b>
     * <ul>
     *   <li>{@code name} – case-insensitive substring match on the court location name.</li>
     *   <li>{@code courtType} – exact match.</li>
     *   <li>{@code startDateFrom}/{@code startDateTo} – constrain the {@code start_date} (inclusive).</li>
     *   <li>{@code endDateFrom}/{@code endDateTo} – constrain the {@code end_date} (inclusive).</li>
     * </ul>
     *
     * <p><b>Date format:</b> All dates are ISO-8601 ({@code yyyy-MM-dd}). When a given end of a
     * range is omitted, only the other bound is applied. If both ends are supplied, the controller
     * validates that {@code from <= to}.
     *
     * <p><b>Sorting:</b> Fixed to {@code name ASC}.
     *
     * <p><b>Responses:</b>
     * <ul>
     *   <li>{@code 200 OK} with {@link CourtLocationPageResponse} on success.</li>
     *   <li>{@code 400 Bad Request} when input validation fails (see class-level notes).</li>
     * </ul>
     *
     * @param name          optional case-insensitive substring filter on court location name
     * @param courtType     optional exact-match court type filter
     * @param page          optional 1-based page number; defaults to {@value #DEFAULT_PAGE}
     * @param pageSize      optional page size; defaults to {@value #DEFAULT_PAGE_SIZE}; must be
     *                      {@code 1..}{@value #MAX_PAGE_SIZE}
     * @param startDateFrom optional lower bound for {@code start_date} (inclusive)
     * @param startDateTo   optional upper bound for {@code start_date} (inclusive)
     * @param endDateFrom   optional lower bound for {@code end_date} (inclusive)
     * @param endDateTo     optional upper bound for {@code end_date} (inclusive)
     * @return {@link ResponseEntity} containing {@link CourtLocationPageResponse} or {@code 400}
     */
    @Operation(summary = "Get court locations (paginated, filterable)",
        operationId = "getCourtLocations")
    @ApiResponse(responseCode = "200", description = "List of court locations retrieved successfully")
    @GetMapping
    public ResponseEntity<CourtLocationPageResponse> list(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String courtType,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo) {

        // Apply defaults for pagination inputs (public API is 1-based).
        final int p = requireNonNullElse(page, DEFAULT_PAGE);
        final int s = requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);

        // Basic pagination validation.
        if (p < 1 || s < 1 || s > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest().build();
        }

        // Validate date ranges only when both ends are provided.
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            return ResponseEntity.badRequest().build();
        }
        if (endDateFrom != null && endDateTo != null && endDateFrom.isAfter(endDateTo)) {
            return ResponseEntity.badRequest().build();
        }

        // Convert to Spring Data's 0-based paging and apply fixed sort by name ASC.
        Pageable pageable = PageRequest.of(p - 1, s, Sort.by("name").ascending());

        // Delegate to service with all filters + pageable; the service composes Specifications.
        Page<CourtLocationDto> pageDto = service.searchCourtLocations(
            name, courtType, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable);

        // Wrap the Spring Page into the API's response model, preserving 1-based page number.
        CourtLocationPageResponse body = new CourtLocationPageResponse(
            pageDto.getContent(), pageDto.getTotalElements(), p, s);

        return ResponseEntity.ok(body);
    }

    /**
     * Fetches a single court location by its identifier.
     *
     * <p>On missing entity the service is expected to throw a
     * {@code ResponseStatusException(HttpStatus.NOT_FOUND)} which Spring maps to a {@code 404}.
     *
     * @param id the identifier of the court location
     * @return {@link ResponseEntity} with a {@link CourtLocationDto} for {@code 200 OK}
     * @see uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService#findById(Long)
     */
    @Operation(summary = "Get a specific court location by ID",
        operationId = "getCourtLocationById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Court location found"),
        @ApiResponse(responseCode = "404", description = "Court location not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourtLocationDto> getById(@PathVariable Long id) {
        // Delegate to service; exceptions are propagated and translated by Spring.
        return ResponseEntity.ok(service.findById(id));
    }
}
