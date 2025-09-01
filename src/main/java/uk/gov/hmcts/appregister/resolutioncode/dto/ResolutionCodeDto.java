package uk.gov.hmcts.appregister.resolutioncode.dto;

import java.time.LocalDate;

/**
 * Immutable Data Transfer Object (DTO) representing a Resolution (Result) Code.
 *
 * <p>This record is the API-facing representation of a resolution code, decoupled from the JPA
 * entity model. It exposes only the fields required by frontend clients and integration consumers,
 * ensuring:
 *
 * <ul>
 *   <li>Persistence details (table/column names, JPA annotations) remain hidden.
 *   <li>The API contract is stable, even if the database schema evolves.
 *   <li>All fields are immutable, since this is defined as a Java {@code record}.
 * </ul>
 *
 * <p><strong>Usage:</strong> Returned directly in responses for:
 *
 * <ul>
 *   <li>Fetching a single resolution code by its unique code value.
 *   <li>Admin and ALE workflows where full metadata is required.
 * </ul>
 *
 * <p><strong>Mapping:</strong> Typically created by a {@code ResolutionCodeMapper} that converts
 * from {@code ResolutionCode} JPA entities.
 */
public record ResolutionCodeDto(

        /**
         * Primary key identifier of the resolution code (maps to {@code rc_id} in the database).
         */
        Long id,

        /** Business code string (e.g., "RC123") used for identification in logic and UI. */
        String resultCode,

        /** Human-readable title of the resolution code. */
        String title,

        /** Full descriptive wording or explanation of the resolution code. */
        String wording,

        /** Optional legislation reference associated with the code. */
        String legislation,

        /** First destination email address for notifications linked to this code, if defined. */
        String destinationEmail1,

        /** Second destination email address for notifications linked to this code, if defined. */
        String destinationEmail2,

        /** Start date (inclusive) from which this code is valid. */
        LocalDate startDate,

        /** End date (inclusive) until which this code is valid, or {@code null} if ongoing. */
        LocalDate endDate) {}
