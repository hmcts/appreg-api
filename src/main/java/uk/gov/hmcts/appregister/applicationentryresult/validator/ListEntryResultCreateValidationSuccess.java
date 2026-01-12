package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * The state of the validation success for @{link ApplicationEntryResultCreationValidator}.
 */
@Builder
@Getter
@Setter
public class ListEntryResultCreateValidationSuccess {
    ApplicationListEntry entry;
    Long resolutionCodeId;
    WordingTemplateSentence template;
}

