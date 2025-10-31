package uk.gov.hmcts.appregister.common.projection;

import java.util.UUID;

public interface ApplicationListEntryPrintProjection {

    UUID getUuid();

    short getSequenceNumber();

    String getApplicantTitle();

    String getApplicantSurname();

    String getApplicantForename1();

    String getApplicantForename2();

    String getApplicantForename3();

    String getApplicantAddress1();

    String getApplicantAddress2();

    String getApplicantAddress3();

    String getApplicantAddress4();

    String getApplicantAddress5();

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

    String getRespondentAddress1();

    String getRespondentAddress2();

    String getRespondentAddress3();

    String getRespondentAddress4();

    String getRespondentAddress5();

    String getRespondentPostcode();

    String getRespondentPhone();

    String getRespondentMobile();

    String getRespondentEmail();

    String getRespondentName();

    String getApplicationCode();

    String getApplicationTitle();

    String getApplicationWording();

    String getCaseReference();

    String getAccountReference();

    String getNotes();
}
