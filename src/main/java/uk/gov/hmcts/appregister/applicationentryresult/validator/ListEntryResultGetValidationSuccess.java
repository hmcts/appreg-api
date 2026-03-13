package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;

/**
 * Validator responsible for ensuring that an application list, list entry is suitable for a get
 * operation.
 */
@Builder
@Getter
@Setter
public classListEntryResultGetValidationSuccess {
    ApplicationList applicationList;
    ApplicationListEntry applicationListEntry;
}
