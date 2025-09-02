package uk.gov.hmcts.appregister.shared.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Unit tests for {@link DateRangeValidator}.
class DateRangeValidatorTest {

    private final DateRangeValidator validator = new DateRangeValidator();

    private static LocalDate ld(int y, int m, int d) { // helper for LocalDate.of
        return LocalDate.of(y, m, d);
    }

    @Test
    @DisplayName("Null ranges are allowed")
    void nullRanges_ok() {
        assertDoesNotThrow(() -> validator.validate(new DateRangeRequest(null, null)));
    }

    @Test
    @DisplayName("Open-ended ranges are allowed (null bounds)")
    void openEnded_ok() {
        DateRange start = new DateRange(ld(2025, 1, 1), null);
        DateRange end = new DateRange(null, ld(2025, 12, 31));
        assertDoesNotThrow(() -> validator.validate(new DateRangeRequest(start, end)));
    }

    @Test
    @DisplayName("Equal bounds are allowed (from == to)")
    void equalBounds_ok() {
        DateRange start = new DateRange(ld(2025, 3, 3), ld(2025, 3, 3));
        assertDoesNotThrow(() -> validator.validate(new DateRangeRequest(start, null)));
    }

    @Test
    @DisplayName("Inverted start range throws 400 with clear message")
    void invertedStart_throws() {
        DateRange start = new DateRange(ld(2025, 2, 2), ld(2025, 1, 1)); // from > to
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> validator.validate(new DateRangeRequest(start, null)));
        assertEquals(400, ex.getStatusCode().value());
        assertEquals("startDateFrom must be on or before startDateTo", ex.getReason());
    }

    @Test
    @DisplayName("Inverted end range throws 400 with clear message")
    void invertedEnd_throws() {
        DateRange end = new DateRange(ld(2025, 5, 10), ld(2025, 5, 1)); // from > to
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> validator.validate(new DateRangeRequest(null, end)));
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("endDateFrom must be on or before endDateTo", ex.getReason());
    }

    @Test
    @DisplayName("Both ranges valid: no exception")
    void bothValid_ok() {
        DateRange start = new DateRange(ld(2025, 1, 1), ld(2025, 1, 31));
        DateRange end = new DateRange(ld(2025, 2, 1), ld(2025, 2, 28));
        assertDoesNotThrow(() -> validator.validate(new DateRangeRequest(start, end)));
    }

    @Test
    @DisplayName("Adapter overload delegates (valid input)")
    void adapterValid_ok() {
        assertDoesNotThrow(
                () ->
                        validator.validate(
                                ld(2025, 1, 1), ld(2025, 1, 31),
                                ld(2025, 2, 1), ld(2025, 2, 28)));
    }

    @Test
    @DisplayName("Adapter overload throws for inverted input")
    void adapterInverted_throws() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                validator.validate(
                                        ld(2025, 3, 10),
                                        ld(2025, 3, 1), // inverted start
                                        null,
                                        null));
        assertEquals(400, ex.getStatusCode().value());
        assertEquals("startDateFrom must be on or before startDateTo", ex.getReason());
    }

    @Test
    @DisplayName("Null request results in NPE (current behavior)")
    void nullRequest_npe() {
        assertThrows(NullPointerException.class, () -> validator.validate((DateRangeRequest) null));
    }
}
