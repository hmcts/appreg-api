package uk.gov.hmcts.appregister.applicationentryresult.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;

/**
 * A successful output come of {@link uk.gov.hmcts.appregister.applicationlist.validator
 * .ApplicationEntryResultDeletionValidator}.
 */
@Getter
@RequiredArgsConstructor
@Setter
public class ListEntryResultDeleteValidationSuccess {
    /** The application list entry result being deleted. */
    private AppListEntryResolution appListEntryResolution;
}
