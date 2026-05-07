package uk.gov.hmcts.appregister.common.projection;

import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

public interface ApplicationListEntryResultWithResultCodeProjection {
    AppListEntryResolution getResolution();

    ResolutionCode getResolutionCode();
}
