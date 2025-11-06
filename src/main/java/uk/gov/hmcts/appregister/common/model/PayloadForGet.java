package uk.gov.hmcts.appregister.common.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * A payload that represents a code and a date for GET requests.
 */
@Getter
@Builder
public class PayloadForGet {
    String code;
    LocalDate date;
}
