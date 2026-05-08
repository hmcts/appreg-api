package uk.gov.hmcts.appregister.applicationentry.validator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

/**
 * The state of the validation success for @{link UpdateApplicationEntryClosedValidation}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UpdateApplicationEntryClosedValidationSuccess {
    private ApplicationList applicationList;
    private ApplicationListEntry applicationEntryId;
}
