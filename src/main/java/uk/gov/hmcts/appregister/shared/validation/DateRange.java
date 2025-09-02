package uk.gov.hmcts.appregister.shared.validation;

import java.time.LocalDate;

/**
 * Immutable date range (inclusive by convention). Bounds may be {@code null} for open-ended ranges;
 * validation is external.
 *
 * @param from start date (nullable)
 * @param to end date (nullable)
 */
public record DateRange(LocalDate from, LocalDate to) {
    // carrier type only
}
