package uk.gov.hmcts.appregister.common.projection;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;

public interface ApplicationListEntryResolutionProjection {

    /** The Application List Entry ID (ale.id). */
    Long getEntryId();

    /** The resolution code associated with the entry. */
    ResolutionCode getResolutionCode();
}
