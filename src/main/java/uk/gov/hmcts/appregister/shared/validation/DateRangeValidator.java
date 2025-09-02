package uk.gov.hmcts.appregister.shared.validation;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates {@link DateRangeRequest} so each range has {@code from <= to}. Throws 400 (Bad Request)
 * when a range is inverted.
 */
@Component
public class DateRangeValidator implements Validator<DateRangeRequest> {

    /**
     * Validate a single named range.
     *
     * @param label field prefix used in error text (e.g. "startDate")
     * @param range the range to check; {@code null} is treated as "no constraint"
     * @throws ResponseStatusException if {@code from} is after {@code to}
     */
    private void validateRange(String label, DateRange range) {
        if (range == null) { // nothing to validate
            return;
        }
        LocalDate from = range.from();
        LocalDate to = range.to();
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, label + "From must be on or before " + label + "To");
        }
    }

    /**
     * Validate both start and end date ranges.
     *
     * @param req the request containing optional ranges
     * @throws ResponseStatusException if any range is inverted
     */
    @Override
    public void validate(DateRangeRequest req) {
        validateRange("startDate", req.start());
        validateRange("endDate", req.end());
    }

    /**
     * Back-compat overload: wraps four dates into a {@link DateRangeRequest}.
     *
     * @param startDateFrom lower bound for startDate (nullable)
     * @param startDateTo upper bound for startDate (nullable)
     * @param endDateFrom lower bound for endDate (nullable)
     * @param endDateTo upper bound for endDate (nullable)
     * @throws ResponseStatusException if any constructed range is inverted
     */
    public void validate(
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo) {
        // Construct ranges and delegate to the main validator
        validate(
                new DateRangeRequest(
                        new DateRange(startDateFrom, startDateTo),
                        new DateRange(endDateFrom, endDateTo)));
    }
}
