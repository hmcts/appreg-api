package uk.gov.hmcts.appregister.courtlocation.dto;

import java.util.List;

/**
 * Response wrapper for paginated court location queries.
 *
 * <p>This record defines the shape of the API response returned by {@code GET /court-locations}. It
 * ensures clients receive both the data and the relevant paging metadata in a predictable
 * structure.
 *
 * <p>Fields:
 *
 * <ul>
 *   <li>{@code results} – the list of court locations returned for the current page.
 *   <li>{@code totalCount} – the total number of records that match the filters (not just this
 *       page).
 *   <li>{@code page} – the current page number (1-based, as exposed by the API).
 *   <li>{@code pageSize} – the number of records per page.
 * </ul>
 *
 * <p>This DTO is separate from Spring Data’s {@link org.springframework.data.domain.Page} to
 * provide a stable, frontend-friendly contract (avoiding Spring-specific field names like {@code
 * content}, {@code totalElements}, etc.).
 *
 * @param results List of {@link CourtLocationDto} items for this page (may be empty).
 * @param totalCount Total number of matching records across all pages.
 * @param page Current page number, 1-based.
 * @param pageSize Size of the page requested.
 */
public record CourtLocationPageResponse(
        List<CourtLocationDto> results, long totalCount, int page, int pageSize) {}
