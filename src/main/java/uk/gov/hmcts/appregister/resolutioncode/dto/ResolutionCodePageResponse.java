package uk.gov.hmcts.appregister.resolutioncode.dto;

import java.util.List;

/**
 * Wrapper DTO representing a page of {@link ResolutionCodeListItemDto} results.
 *
 * <p>This is the standard paginated response structure returned by {@code GET /result-codes}. It
 * ensures frontend clients receive both the result set and metadata required for pagination
 * controls.
 *
 * <p><strong>Fields:</strong>
 *
 * <ul>
 *   <li>{@code results} – the list of result code list items for the current page.
 *   <li>{@code totalCount} – total number of matching result codes across all pages.
 *   <li>{@code page} – current page number (1-based, aligned with API contract).
 *   <li>{@code pageSize} – number of results per page (must respect max limits).
 * </ul>
 *
 * <p>All fields are immutable since this is a Java {@code record}.
 */
public record ResolutionCodePageResponse(

        /** List of result code items for this page. May be empty if no results match. */
        List<ResolutionCodeListItemDto> results,

        /** Total number of result codes matching the filter across all pages. */
        long totalCount,

        /** Current page number (1-based, consistent with API semantics). */
        int page,

        /** Number of items requested per page (subject to max size constraints). */
        int pageSize) {}
