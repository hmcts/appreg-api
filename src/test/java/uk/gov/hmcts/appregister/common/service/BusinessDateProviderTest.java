package uk.gov.hmcts.appregister.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusinessDateProviderTest {

    @Test
    void currentUkDate_usesConfiguredUkTimezone() {
        Clock utcClock = Clock.fixed(Instant.parse("2025-06-01T23:30:00Z"), ZoneId.of("UTC"));
        BusinessDateProvider provider =
                new BusinessDateProvider(utcClock, ZoneId.of("Europe/London"));

        assertEquals(LocalDate.of(2025, 6, 2), provider.currentUkDate());
    }
}
