package uk.gov.hmcts.appregister.applicationentry.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.FeePair;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * The state of the validation success for @{link CreateApplicationEntryValidator}.
 */
@Builder
@Getter
@Setter
public class CreateApplicationEntryValidationSuccess {
    private WordingTemplateSentence wordingSentence;
    private ApplicationCode applicationCode;
    private FeePair fee;
    private StandardApplicant sa;
    private ApplicationList applicationList;
}
