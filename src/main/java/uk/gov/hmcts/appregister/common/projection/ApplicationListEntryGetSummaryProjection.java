package uk.gov.hmcts.appregister.common.projection;

import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

import java.time.LocalDate;

public interface ApplicationListEntryGetSummaryProjection {
    String getUuid();
    LocalDate getHearingDate();
    String getCourtCode();
    String getLegislation();
    boolean isFreeRequired();
    String getResult();
    String getCjaCode();
    String getOtherLocationDescription();
    String getAppOrganisation();
    String getAppSurname();
    NameAddress getAnamedaddress();
    String getStandardApplicantCode();
    String getRespondentOrganisation();
    String getRespondentSurname();
    String getRespondentPostcode();
    NameAddress getRspondentAddress();
    String getAccountReference();
    Status getStatus();
    String getApplicationTitle();
}
