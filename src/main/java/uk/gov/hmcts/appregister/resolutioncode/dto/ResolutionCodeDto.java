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
        Long id,
        String resultCode,
        String title,
        String wording,
        String legislation,
        String destinationEmail1,
        String destinationEmail2,
        LocalDate startDate,
        LocalDate endDate) {}
