package uk.gov.hmcts.appregister.shared.validation;

/**
 * Pair of date ranges: one for "startDate" and one for "endDate". Validation and semantics (e.g.,
 * inclusivity) are handled elsewhere.
 *
 * @param start range applied to startDate (nullable)
 * @param end range applied to endDate (nullable)
 */
public record DateRangeRequest(DateRange start, DateRange end) {
    // data carrier only
}
