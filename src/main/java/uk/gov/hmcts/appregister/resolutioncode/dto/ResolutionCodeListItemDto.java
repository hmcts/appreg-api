package uk.gov.hmcts.appregister.resolutioncode.dto;

/**
 * Lightweight projection DTO for listing result codes.
 *
 * <p>This is used in paginated search/list endpoints where only a subset of fields is needed (ID,
 * code, title) rather than the full metadata exposed by {@link ResolutionCodeDto}.
 *
 * <p>Benefits of a list item DTO:
 *
 * <ul>
 *   <li>Smaller payloads for list views in Admin or ALE workflows.
 *   <li>Reduced coupling — clients only rely on the fields relevant to selection/search screens.
 *   <li>Stable contract independent of internal database entity structure.
 * </ul>
 *
 * <p>All fields are immutable since this is declared as a Java {@code record}.
 */
public record ResolutionCodeListItemDto(

        /** Primary key identifier of the result code (maps to {@code rc_id}). */
        Long id,

        /** Short code value (e.g., "RC123") uniquely identifying the result code. */
        String code,

        /** Human-readable title/description shown in list screens. */
        String title) {}
