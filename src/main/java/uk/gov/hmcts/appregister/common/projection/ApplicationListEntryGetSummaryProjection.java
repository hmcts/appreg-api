package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

public interface ApplicationListEntryGetSummaryProjection {
    String getUuid();

    String getCourtCode();

    String getLegislation();

    YesOrNo getFeeRequired();

    String getResult();

    String getCjaCode();

    String getOtherLocationDescription();

    NameAddress getAnameAddress();

    String getStandardApplicantCode();

    NameAddress getRnameAddress();

    Status getStatus();

    String getTitle();

    LocalDate getDateOfAl();

    String getApplicationOrganisation();

    String getApplicantSurname();

    String getRespondentOrganisation();

    String getRespondentSurname();

    String getRespondentPostcode();

    String getAccountReference();

    StandardApplicant getStandardApplicant();

    Long getId();

    String getApplicationListId();
}
