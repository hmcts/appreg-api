package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

public interface ApplicationListEntryGetSummaryProjection {
    String getUuid();

    String getCourtCode();

    String getLegislation();

    YesOrNo getFeeRequired();

    String getCjaCode();

    String getOtherLocationDescription();

    NameAddress getAnameAddress();

    /** Applicant display name for sorting/filtering summaries. */
    String getApplicantName();

    String getStandardApplicantCode();

    NameAddress getRnameAddress();

    /** Respondent display name for sorting/filtering summaries. */
    String getRespondentName();

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

    String getListId();

    Integer getSequenceNumber();

    List<ResolutionCode> getResolutionCodes();
}
