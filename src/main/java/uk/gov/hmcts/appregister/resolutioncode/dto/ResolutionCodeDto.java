package uk.gov.hmcts.appregister.resolutioncode.dto;

import java.time.LocalDate;

/**
 * API-facing Data Transfer Object (DTO) representing a Result Code record.
 *
 * <p>This is the "read model" returned to clients when fetching a result code (via GET endpoints).
 * It deliberately decouples the REST API contract from the JPA entity so that:
 *
 * <ul>
 *   <li>Internal persistence details are hidden from consumers.
 *   <li>The contract remains stable even if the database schema changes.
 *   <li>Only relevant fields are exposed to frontend clients.
 * </ul>
 *
 * <p>All fields are immutable, as this is declared as a Java {@code record}.
 */
public record ResolutionCodeDto(

        /** Primary key identifier of the result code (maps to {@code rc_id}). */
        Long id,

        /** Short code value (e.g. "RC123"), used in application logic and UI. */
        String resultCode,

        /** Human-readable title of the result code. */
        String title,

        /** Full textual wording or description of the result code. */
        String wording,

        /** Legislation reference associated with this result code (nullable). */
        String legislation,

        /** First destination email address linked to this result code (nullable). */
        String destinationEmail1,

        /** Second destination email address linked to this result code (nullable). */
        String destinationEmail2,

        /** Start date from which this result code is valid (inclusive). */
        LocalDate startDate,

        /** Optional end date until which this result code is valid (inclusive). */
        LocalDate endDate) {}
