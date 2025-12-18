package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;

public interface ApplicationListEntryPrintProjection {

    Long getId();

    short getSequenceNumber();

    String getApplicantTitle();

    String getApplicantSurname();

    String getApplicantForename1();

    String getApplicantForename2();

    String getApplicantForename3();

    String getApplicantAddressLine1();

    String getApplicantAddressLine2();

    String getApplicantAddressLine3();

    String getApplicantAddressLine4();

    String getApplicantAddressLine5();

    String getApplicantPostcode();

    String getApplicantPhone();

    String getApplicantMobile();

    String getApplicantEmail();

    String getApplicantName();

    String getRespondentTitle();

    String getRespondentSurname();

    String getRespondentForename1();

    String getRespondentForename2();

    String getRespondentForename3();

    String getRespondentAddressLine1();

    String getRespondentAddressLine2();

    String getRespondentAddressLine3();

    String getRespondentAddressLine4();

    String getRespondentAddressLine5();

    String getRespondentPostcode();

    String getRespondentPhone();

    String getRespondentMobile();

    String getRespondentEmail();

    LocalDate getRespondentDateOfBirth();

    String getRespondentName();

    String getApplicationCode();

    String getApplicationTitle();

    String getApplicationWording();

    String getCaseReference();

    String getAccountReference();

    String getNotes();
}
