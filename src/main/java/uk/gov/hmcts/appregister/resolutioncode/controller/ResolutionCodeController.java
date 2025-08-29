package uk.gov.hmcts.appregister.resolutioncode.controller;

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
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodePageResponse;
import uk.gov.hmcts.appregister.resolutioncode.service.ResolutionCodeService;

/**
 * REST controller exposing read-only endpoints for Result Codes.
 *
 * <p>Features:
 *
 * <ul>
 *   <li><strong>Paginated, filterable listing</strong> of result codes for Admin and ALE flows.
 *   <li><strong>Fetch-by-code</strong> endpoint returning full metadata for a single result code.
 * </ul>
 *
 * <p><strong>Pagination model:</strong> The public API is 1-based (i.e., {@code page=1} is the
 * first page), while Spring Data is 0-based. This controller converts to 0-based internally.
 *
 * <p><strong>Validation:</strong> Returns {@code 400 Bad Request} when:
 *
 * <ul>
 *   <li>{@code page < 1}
 *   <li>{@code pageSize < 1} or {@code pageSize > MAX_PAGE_SIZE}
 *   <li>{@code startDateFrom > startDateTo}
 *   <li>{@code endDateFrom > endDateTo}
 * </ul>
 *
 * <p><strong>Sorting:</strong> Listing is sorted by {@code title ASC} to provide deterministic UI
 * order.
 */
@RestController
@RequestMapping("/resolution-code")
@RequiredArgsConstructor
public class ResolutionCodeController {

    // Default 1-based page number exposed by the public API.
    private static final int DEFAULT_PAGE = 1;

    // Default page size when caller does not specify {@code pageSize}.
    private static final int DEFAULT_PAGE_SIZE = 10;

    // Upper bound on page size to protect the service from overly large requests.
    private static final int MAX_PAGE_SIZE = 100;

    // Application service that encapsulates search and lookup logic.
    private final ResolutionCodeService service;

    /**
     * Returns a paginated, filterable list of result codes.
     *
     * <p><strong>Filters:</strong>
     *
     * <ul>
     *   <li>{@code code} – case-insensitive partial match (ILIKE semantics).
     *   <li>{@code title} – case-insensitive partial match.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range on {@code startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range on {@code endDate}.
     * </ul>
     *
     * <p><strong>Dates:</strong> ISO-8601 ({@code yyyy-MM-dd}). When only one end of a range is
     * provided, only that bound is applied. If both ends are provided, the controller validates
     * {@code from <= to}.
     *
     * <p><strong>Sorting:</strong> Fixed to {@code title ASC}.
     *
     * <p><strong>Responses:</strong>
     *
     * <ul>
     *   <li>{@code 200 OK} with {@link ResolutionCodePageResponse} on success.
     *   <li>{@code 400 Bad Request} when validation fails (see rules above).
     * </ul>
     *
     * @param code optional case-insensitive substring filter on result code value
     * @param title optional case-insensitive substring filter on title
     * @param startDateFrom optional lower bound (inclusive) for {@code startDate}
     * @param startDateTo optional upper bound (inclusive) for {@code startDate}
     * @param endDateFrom optional lower bound (inclusive) for {@code endDate}
     * @param endDateTo optional upper bound (inclusive) for {@code endDate}
     * @param page optional 1-based page number (defaults to {@value #DEFAULT_PAGE})
     * @param pageSize optional page size (defaults to {@value #DEFAULT_PAGE_SIZE}, max {@value
     *     #MAX_PAGE_SIZE})
     * @return {@link ResponseEntity} with {@link ResolutionCodePageResponse} or {@code 400 Bad Request}
     */
    @Operation(summary = "Get result codes (paginated, filterable)")
    @ApiResponse(responseCode = "200", description = "List of result codes retrieved successfully")
    @GetMapping
    public ResponseEntity<ResolutionCodePageResponse> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDateTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        // Apply default pagination values (public API is 1-based).
        final int p = requireNonNullElse(page, DEFAULT_PAGE);
        final int s = requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);

        // Basic input validation for pagination arguments.
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

        // Convert to Spring Data's 0-based paging and apply fixed sort by title ASC.
        final Pageable pageable = PageRequest.of(p - 1, s, Sort.by("title").ascending());

        // Delegate to service: it composes Specifications and performs the search.
        final Page<ResolutionCodeListItemDto> pageDto =
                service.search(
                        code, title, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable);

        // Wrap Spring Page into API response model, preserving 1-based page number.
        final ResolutionCodePageResponse body =
                new ResolutionCodePageResponse(pageDto.getContent(), pageDto.getTotalElements(), p, s);

        return ResponseEntity.ok(body);
    }

    /**
     * Retrieves full metadata for a single result code identified by its code value.
     *
     * <p>On missing entity, the service is expected to throw {@code
     * ResponseStatusException(HttpStatus.NOT_FOUND)}, which Spring maps to {@code 404}.
     *
     * @param code the unique result code value to look up
     * @return {@link ResponseEntity} with {@link ResolutionCodeDto} for {@code 200 OK}
     */
    @Operation(summary = "Get a result code by code", operationId = "getResultCodeByCode")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Result code found"),
        @ApiResponse(responseCode = "404", description = "Result code not found")
    })
    @GetMapping("/{code}")
    public ResponseEntity<ResolutionCodeDto> getByCode(@PathVariable String code) {
        // Delegate to service; exceptions (e.g., 404) are propagated and translated by Spring.
        return ResponseEntity.ok(service.findByCode(code));
    }
}
