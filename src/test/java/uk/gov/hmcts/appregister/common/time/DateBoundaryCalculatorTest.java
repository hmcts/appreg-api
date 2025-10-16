package uk.gov.hmcts.appregister.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateBoundaryCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private DateBoundaryCalculator calculator;

    @BeforeEach
    void setUp() {
        // Fixed clock: 2025-10-16T10:00 in London
        Clock fixedClock =
                Clock.fixed(LocalDateTime.of(2025, 10, 16, 10, 0).atZone(ZONE).toInstant(), ZONE);
        calculator = new DateBoundaryCalculator(ZONE, fixedClock);
    }

    @Test
    void startOfDay_shouldReturnMidnightInConfiguredZone() {
        LocalDate date = LocalDate.of(2025, 10, 16);

        LocalDateTime start = calculator.startOfDay(date);

        assertThat(start).isEqualTo(LocalDateTime.of(2025, 10, 16, 0, 0));
    }

    @Test
    void startOfNextDay_shouldReturnMidnightOfFollowingDay() {
        LocalDate date = LocalDate.of(2025, 10, 16);

        LocalDateTime nextStart = calculator.startOfNextDay(date);

        assertThat(nextStart).isEqualTo(LocalDateTime.of(2025, 10, 17, 0, 0));
    }

    @Test
    void getToday_shouldReturnCurrentDateFromClockAndZone() {
        LocalDate today = calculator.getToday();

        assertThat(today).isEqualTo(LocalDate.of(2025, 10, 16));
    }
}
