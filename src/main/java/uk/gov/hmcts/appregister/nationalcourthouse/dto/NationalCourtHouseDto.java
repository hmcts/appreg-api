package uk.gov.hmcts.appregister.nationalcourthouse.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing a Court Location.
 *
 * <p>This DTO is designed to decouple API responses from the JPA entity model. It carries only the
 * data needed by clients of the API, in a flattened structure.
 *
 * <p>Fields are aligned with the {@code national_court_houses} table, but names have been
 * normalised for easier consumption on the frontend.
 *
 * @param id Unique identifier for the court location
 * @param name The name of the courthouse (e.g. "Cardiff Crown Court")
 * @param courtType The type of court (e.g. "CROWN", "MAGISTRATES")
 * @param startDate The date from which this courthouse record is valid
 * @param endDate The date until which this courthouse record is valid, or {@code null} if ongoing
 * @param locationId Foreign key reference to the linked location record
 * @param psaId Foreign key reference to a petty sessions area (PSA) if applicable
 * @param courtLocationCode Business code for the court location, used for integration and reference
 *     data
 * @param welshName The Welsh-language name of the courthouse, if defined
 * @param orgId Organisation identifier, used to group or relate court locations at organisational
 *     level
 */
public record NationalCourtHouseDto(
        Long id,
        String name,
        String courtType,
        LocalDate startDate,
        LocalDate endDate,
        Long locationId,
        Long psaId,
        String courtLocationCode,
        String welshName,
        Long orgId) {}
