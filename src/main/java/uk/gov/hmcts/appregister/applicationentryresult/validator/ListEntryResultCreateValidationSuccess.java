package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * The state of the validation success for @{link ApplicationEntryResultCreationValidator}.
 */
@Builder
@Getter
@Setter
public class ListEntryResultCreateValidationSuccess {
    ApplicationListEntry applicationListEntry;
    ResolutionCode resolutionCode;
    WordingTemplateSentence wordingSentence;
}
