package uk.gov.hmcts.appregister.resolutioncode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import uk.gov.hmcts.appregister.resolutioncode.service.ResolutionCodeService;
import uk.gov.hmcts.appregister.shared.validation.DateRangeValidator;

/**
 * REST controller exposing read-only endpoints for Resolution (Result) Codes.
 *
 * <p>Features:
 *
 * <ul>
 *   <li><strong>Paginated, filterable listing</strong> for Admin and ALE flows.
 *   <li><strong>Fetch-by-code</strong> endpoint returning full metadata for a single code.
 * </ul>
 *
 * <p><strong>Pagination</strong>: Spring injects a {@link Pageable} which is <em>zero-based</em> by
 * default (page 0 is the first page). A default sort of <code>title ASC</code> is applied via
 * {@code @PageableDefault} so the UI gets deterministic ordering without having to specify it.
 *
 * <p><strong>Validation</strong>: Date range inputs are validated by {@link DateRangeValidator}. If
 * an invalid range is supplied (e.g., {@code startDateFrom > startDateTo}), a {@code 400} is
 * returned.
 */
@RestController
@RequestMapping("/resolution-codes")
@RequiredArgsConstructor
public class ResolutionCodeController {

    /** Application service that encapsulates search and lookup logic. */
    private final ResolutionCodeService service;

    /** Centralised validator for start/end date ranges. */
    private final DateRangeValidator dateRangeValidator;

    /**
     * Returns a paginated, filterable list of resolution codes.
     *
     * <p><strong>Filters:</strong>
     *
     * <ul>
     *   <li>{@code code} – case-insensitive partial match.
     *   <li>{@code title} – case-insensitive partial match.
     *   <li>{@code startDateFrom}/{@code startDateTo} – inclusive range on {@code startDate}.
     *   <li>{@code endDateFrom}/{@code endDateTo} – inclusive range on {@code endDate}.
     * </ul>
     *
     * <p><strong>Dates:</strong> ISO-8601 ({@code yyyy-MM-dd}). Supplying only one bound applies a
     * single-sided range; supplying both bounds must satisfy {@code from <= to} (validated here).
     *
     * @return {@code 200 OK} with a {@link Page} of {@link ResolutionCodeListItemDto}.
     */
    @Operation(summary = "Get result codes (paginated, filterable)")
    @ApiResponse(responseCode = "200", description = "List of result codes retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<ResolutionCodeListItemDto>> list(
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
            // Default sort: title ASC; clients can override with ?sort=field,desc etc.
            @org.springframework.data.web.PageableDefault(
                            sort = "title",
                            direction = Sort.Direction.ASC)
                    Pageable pageable) {
        // Delegate range checks (returns 400 via exception on invalid input).
        dateRangeValidator.validate(startDateFrom, startDateTo, endDateFrom, endDateTo);

        // Delegate search to the service; page metadata (size, number, total) is preserved.
        Page<ResolutionCodeListItemDto> page =
                service.search(
                        code, title, startDateFrom, startDateTo, endDateFrom, endDateTo, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Retrieve a single resolution code by its ID.
     *
     * <p>If the ID does not exist, the service raises a {@code ResponseStatusException(404)} which
     * Spring maps to {@code 404 Not Found}.
     *
     * @param id database identifier of the resolution code
     * @return {@code 200 OK} with the resolution code DTO; {@code 404 Not Found} if missing
     */
    @Operation(
            summary = "Get a specific resolution code by ID",
            operationId = "getResolutionCodeById")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resolution code found"),
        @ApiResponse(responseCode = "404", description = "Resolution code not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResolutionCodeDto> getById(@PathVariable Long id) {
        // Service throws 404 if not found; we simply return 200 with the DTO on success.
        return ResponseEntity.ok(service.findById(id));
    }
}
