package uk.gov.hmcts.appregister.applicationentry.validator;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * The state of the validation success for @{link CreateApplicationEntryValidator}.
 */
@Getter
@Setter
public class UpdateApplicationEntryValidationSuccess
        extends CreateApplicationEntryValidationSuccess {
    public UpdateApplicationEntryValidationSuccess(
            WordingTemplateSentence wordingSentence,
            ApplicationCode applicationCode,
            Fee fee,
            StandardApplicant sa,
            ApplicationList applicationList,
            ApplicationListEntry applicationEntryId) {
        super(wordingSentence, applicationCode, fee, sa, applicationList);
        this.applicationEntryId = applicationEntryId;
    }

    private ApplicationListEntry applicationEntryId;
}
