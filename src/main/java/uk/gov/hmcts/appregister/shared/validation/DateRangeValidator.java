package uk.gov.hmcts.appregister.shared.validation;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates optional date ranges commonly used by list endpoints. Throws a 400 if a provided
 * "from..to" pair is inverted.
 */
@Component
public class DateRangeValidator {

    public void validate(
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDate endDateFrom,
            LocalDate endDateTo) {
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "startDateFrom must be on or before startDateTo");
        }
        if (endDateFrom != null && endDateTo != null && endDateFrom.isAfter(endDateTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "endDateFrom must be on or before endDateTo");
        }
    }
}
