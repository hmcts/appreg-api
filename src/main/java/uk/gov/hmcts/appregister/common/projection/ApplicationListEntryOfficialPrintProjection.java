package uk.gov.hmcts.appregister.common.projection;

import uk.gov.hmcts.appregister.common.enumeration.OfficialType;

public interface ApplicationListEntryOfficialPrintProjection {

    Long getEntryId();

    OfficialType getType();

    String getTitle();

    String getForename();

    String getSurname();
}
