package uk.gov.hmcts.appregister.resolutioncode.dto;

/**
 * Lightweight Data Transfer Object (DTO) for listing resolution codes.
 *
 * <p>This record is designed for use in paginated search/list endpoints where only a subset of
 * fields is required, avoiding the overhead of returning the full {@link ResolutionCodeDto}
 * metadata.
 *
 * <p><strong>Why a list item DTO?</strong>
 *
 * <ul>
 *   <li>Minimises payload size for list views in Admin and ALE workflows.
 *   <li>Reduces coupling by exposing only the fields relevant for selection/search screens.
 *   <li>Provides a stable API contract that remains unaffected by changes in the underlying entity
 *       or full DTO model.
 * </ul>
 *
 * <p>Because this is a Java {@code record}, all fields are immutable, making it safe to share in
 * API responses.
 *
 * <p><strong>Usage:</strong> Returned directly in paginated {@link
 * org.springframework.data.domain.Page} responses from search endpoints. Typically constructed via
 * a mapper converting from {@code ResolutionCode} JPA entities.
 */
public record ResolutionCodeListItemDto(

        /** Database identifier of the resolution code (maps to {@code rc_id}). */
        Long id,

        /** Short business code (e.g., "RC123") uniquely identifying the resolution code. */
        String code,

        /** Human-readable title of the resolution code, shown in list screens. */
        String title) {}
