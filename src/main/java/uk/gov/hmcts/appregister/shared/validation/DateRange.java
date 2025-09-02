package uk.gov.hmcts.appregister.shared.validation;

import java.time.LocalDate;

public record DateRange(LocalDate from, LocalDate to) {
}
