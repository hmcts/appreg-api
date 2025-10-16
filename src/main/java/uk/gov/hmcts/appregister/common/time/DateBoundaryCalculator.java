package uk.gov.hmcts.appregister.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility service for calculating date and time boundaries based on a configured {@link ZoneId}.
 *
 * <p>Provides consistent "start of day" and "next day" calculations used across the application for
 * date-window–based queries (e.g. active-from/until filtering). Backed by an injected {@link Clock}
 * to allow deterministic testing.
 */
@Component
@RequiredArgsConstructor
public class DateBoundaryCalculator {

    private final ZoneId zone;
    private final Clock clock;

    /**
     * Returns the start of the specified local date in the configured time zone.
     *
     * <p>Example: for date {@code 2025-10-16} and zone {@code Europe/London}, this returns {@code
     * 2025-10-16T00:00} in that zone, converted to a {@link LocalDateTime}.
     *
     * @param date the date for which to determine the start-of-day boundary
     * @return a {@link LocalDateTime} at midnight of the given date in the configured zone
     */
    public LocalDateTime startOfDay(LocalDate date) {
        return LocalDateTime.ofInstant(date.atStartOfDay(zone).toInstant(), zone);
    }

    /**
     * Returns the start of the next local day in the configured time zone.
     *
     * <p>Equivalent to {@code startOfDay(date.plusDays(1))}.
     *
     * @param date the reference date
     * @return a {@link LocalDateTime} representing midnight at the start of the following day
     */
    public LocalDateTime startOfNextDay(LocalDate date) {
        return LocalDateTime.ofInstant(date.plusDays(1).atStartOfDay(zone).toInstant(), zone);
    }

    /**
     * Returns the current local date ("today") in the configured time zone.
     *
     * <p>Uses the injected {@link Clock} for deterministic time calculations and testability.
     *
     * @return the current {@link LocalDate} according to the configured zone and clock
     */
    public LocalDate getToday() {
        return LocalDate.now(clock.withZone(zone));
    }
}
