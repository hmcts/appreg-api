package uk.gov.hmcts.appregister.common.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class UkDayWindowService {

    private final ZoneId ukZone;
    private final Clock clock;

    /**
     * Start of the UK day represented by the given LocalDate.
     */
    public LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay(ukZone).toLocalDateTime();
    }

    /**
     * Start of the next UK day (exclusive bound for a "day window").
     */
    public LocalDateTime startOfNext(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ukZone).toLocalDateTime();
    }

    /**
     * Start of the next UK day (exclusive bound for a "day window").
     */
    public LocalDate todayUk() {
        return LocalDate.now(clock.withZone(ukZone));
    }
}
