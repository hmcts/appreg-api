package uk.gov.hmcts.appregister.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provides the current business date in the configured UK timezone.
 */
@Component
@RequiredArgsConstructor
public class BusinessDateProvider {

    private final Clock clock;
    private final ZoneId ukZone;

    public LocalDate currentUkDate() {
        return LocalDate.now(clock.withZone(ukZone));
    }
}
