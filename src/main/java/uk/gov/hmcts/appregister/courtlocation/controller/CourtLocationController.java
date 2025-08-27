package uk.gov.hmcts.appregister.courtlocation.controller;

import static java.util.Objects.requireNonNullElse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * <p>Supports paginated and filterable listing for use by client UI (e.g. dropdowns, reference data
 * screens) and a simple fetch-by-id endpoint.
 *
 * <p><strong>Pagination model:</strong> public API is 1-based (page=1 is the first page), while the
 * underlying Spring Data API is 0-based. The controller handles conversion.
 *
 * <p><strong>Validation:</strong> returns {@code 400 Bad Request} if {@code page < 1}, {@code
 * pageSize < 1}, or {@code pageSize > MAX_PAGE_SIZE}.
 */
@RestController
@RequestMapping("/court-locations")
@RequiredArgsConstructor
public class CourtLocationController {

    /** Default page number for public API (1-based). */
    private static final int DEFAULT_PAGE = 1;

    /** Default page size when not specified by the client. */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** Upper bound on page size to protect the service from overly large requests. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Application service used to query court locations. */
    private final CourtLocationService service;

    /**
     * Returns a paginated list of court locations.
     *
     * <p>Filters:
     *
     * <ul>
     *   <li>{@code name} – case-insensitive contains.
     *   <li>{@code courtType} – exact match.
     * </ul>
     *
     * <p>Sorting is fixed to {@code name ASC} for now.
     *
     * @param name optional case-insensitive substring filter on court location name
     * @param courtType optional exact-match court type filter
     * @param page optional 1-based page number; defaults to {@value #DEFAULT_PAGE}
     * @param pageSize optional page size; defaults to {@value #DEFAULT_PAGE_SIZE}; must be {@code
     *     1..}{@value #MAX_PAGE_SIZE}
     * @return {@link ResponseEntity} containing {@link CourtLocationPageResponse} with {@code
     *     results}, {@code totalCount}, {@code page}, {@code pageSize} or {@code 400 Bad Request}
     *     when validation fails
     */
    @Operation(summary = "Get all courthouses", operationId = "getAllCourthouses")
    @ApiResponse(responseCode = "200", description = "List of courthouses retrieved successfully")
    @GetMapping
    public ResponseEntity<CourtLocationPageResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String courtType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        // Apply defaults when null is provided by the client.
        final int p = requireNonNullElse(page, DEFAULT_PAGE);
        final int s = requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);

        // Basic input validation; reject obviously invalid paging parameters early.
        if (p < 1 || s < 1 || s > MAX_PAGE_SIZE) {
            // Keep payload empty for a simple 400 response.
            return ResponseEntity.badRequest().build();
        }

        // Convert public 1-based page -> Spring Data 0-based page.
        // Sort is fixed to "name" ascending as per current requirements.
        final Pageable pageable = PageRequest.of(p - 1, s, Sort.by("name").ascending());

        // Delegate to service which applies filters/specifications and maps to DTOs.
        final Page<CourtLocationDto> pageDto =
                service.searchCourtLocations(name, courtType, pageable);

        // Wrap Spring's Page into a stable API shape (results + metadata).
        final CourtLocationPageResponse body =
                new CourtLocationPageResponse(
                        pageDto.getContent(), // current page results
                        pageDto.getTotalElements(), // total records matching the filters
                        p, // echo public page number (1-based)
                        s // echo page size requested
                        );

        return ResponseEntity.ok(body);
    }

    /**
     * Fetches a single court location by its identifier.
     *
     * <p>On missing entity the service is expected to throw a {@code
     * ResponseStatusException(HttpStatus.NOT_FOUND)} which Spring maps to a 404.
     *
     * @param id the identifier of the court location
     * @return {@link ResponseEntity} with a {@link CourtLocationDto} for {@code 200 OK}
     * @see uk.gov.hmcts.appregister.courtlocation.service.CourtLocationService#findById(Long)
     */
    @Operation(summary = "Get a specific courthouse by ID", operationId = "getCourtHouseById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Courthouse found"),
        @ApiResponse(responseCode = "404", description = "Courthouse not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourtLocationDto> getById(@PathVariable Long id) {
        // Delegate to service; exceptions are propagated and translated by Spring.
        return ResponseEntity.ok(service.findById(id));
    }
}
