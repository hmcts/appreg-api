package uk.gov.hmcts.appregister.applicationcode.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;

/**
 * The validation success for @{link GetApplicationCodeValidator}.
 */
@Builder
@Getter
@Setter
public class GetApplicationCodeValidationSuccess {
    private ApplicationCode applicationCode;
}
