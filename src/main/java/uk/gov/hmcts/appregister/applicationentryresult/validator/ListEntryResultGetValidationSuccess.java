package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;

/**
 * Validator responsible for ensuring that an application list, list entry is suitable for a get
 * operation.
 */
@Builder
@Getter
@Setter
public class ListEntryResultGetValidationSuccess {
    ApplicationList applicationList;
    ApplicationListEntry applicationListEntry;
}
