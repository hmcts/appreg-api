package uk.gov.hmcts.appregister.common.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * A payload that represents a code and a date for GET requests.
 */
@Getter
@Builder
public class PayloadForGet {
    String code;
    LocalDate date;
}
