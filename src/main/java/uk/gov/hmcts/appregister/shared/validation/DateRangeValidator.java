package uk.gov.hmcts.appregister.shared.validation;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DateRangeValidator implements Validator<DateRangeRequest> {

    @Override
    public void validate(DateRangeRequest req) {
        validateRange("startDate", req.start());
        validateRange("endDate",   req.end());
    }

    private void validateRange(String label, DateRange range) {
        if (range == null) return;
        LocalDate from = range.from();
        LocalDate to   = range.to();
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, label + "From must be on or before " + label + "To");
        }
    }

    /** Adapter to keep old call sites working while you migrate. */
    public void validate(LocalDate startDateFrom, LocalDate startDateTo,
                         LocalDate endDateFrom,   LocalDate endDateTo) {
        validate(new DateRangeRequest(
            new DateRange(startDateFrom, startDateTo),
            new DateRange(endDateFrom,   endDateTo)
        ));
    }
}
