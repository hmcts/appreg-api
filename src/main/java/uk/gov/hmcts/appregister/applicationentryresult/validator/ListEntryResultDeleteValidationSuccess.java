package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * The state of the validation success for {@link ApplicationEntryResultDeletionValidator}.
 */
@Getter
@Setter
public class ListEntryResultDeleteValidationSuccess extends ListEntryResultCreateValidationSuccess {
    public ListEntryResultDeleteValidationSuccess(
            WordingTemplateSentence wordingSentence,
            ResolutionCode resultCode,
            ApplicationList applicationList,
            ApplicationListEntry applicationListEntry,
            AppListEntryResolution appListEntryResult) {

        super(applicationList, applicationListEntry, resultCode, wordingSentence);
        this.appListEntryResult = appListEntryResult;
    }

    private AppListEntryResolution appListEntryResult;
}
