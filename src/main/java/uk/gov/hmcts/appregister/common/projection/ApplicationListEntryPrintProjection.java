package uk.gov.hmcts.appregister.common.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface ApplicationListEntryPrintProjection {

    Long getId();

    UUID getUuid();

    UUID getListId();

    short getSequenceNumber();

    String getApplicantTitle();

    String getApplicantLastName();

    String getApplicantFirstName();

    String getApplicantMiddleName();

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

    String getRespondentLastName();

    String getRespondentFirstName();

    String getRespondentMiddleName();

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
