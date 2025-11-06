package uk.gov.hmcts.appregister.util;

import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;

import java.time.OffsetDateTime;

public final class ApplicationListEntryPrintProjectionUtil {

    public static Builder applicationListEntryPrintProjection() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private short sequenceNumber;
        private String applicantTitle;
        private String applicantSurname;
        private String applicantForename1;
        private String applicantForename2;
        private String applicantForename3;
        private String applicantAddressLine1;
        private String applicantAddressLine2;
        private String applicantAddressLine3;
        private String applicantAddressLine4;
        private String applicantAddressLine5;
        private String applicantPostcode;
        private String applicantPhone;
        private String applicantMobile;
        private String applicantEmail;
        private String applicantName;
        private String respondentTitle;
        private String respondentSurname;
        private String respondentForename1;
        private String respondentForename2;
        private String respondentForename3;
        private String respondentAddressLine1;
        private String respondentAddressLine2;
        private String respondentAddressLine3;
        private String respondentAddressLine4;
        private String respondentAddressLine5;
        private String respondentPostcode;
        private String respondentPhone;
        private String respondentMobile;
        private String respondentEmail;
        private OffsetDateTime respondentDateOfBirth;
        private String respondentName;
        private String applicationCode;
        private String applicationTitle;
        private String applicationWording;
        private String caseReference;
        private String accountReference;
        private String notes;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sequenceNumber(int sequenceNumber) {
            this.sequenceNumber = (short) sequenceNumber;
            return this;
        }

        public Builder applicantTitle(String applicantTitle) {
            this.applicantTitle = applicantTitle;
            return this;
        }

        public Builder applicantSurname(String applicantSurname) {
            this.applicantSurname = applicantSurname;
            return this;
        }

        public Builder applicantForename1(String applicantForename1) {
            this.applicantForename1 = applicantForename1;
            return this;
        }

        public Builder applicantForename2(String applicantForename2) {
            this.applicantForename2 = applicantForename2;
            return this;
        }

        public Builder applicantForename3(String applicantForename3) {
            this.applicantForename3 = applicantForename3;
            return this;
        }

        public Builder applicantAddressLine1(String applicantAddressLine1) {
            this.applicantAddressLine1 = applicantAddressLine1;
            return this;
        }

        public Builder applicantAddressLine2(String applicantAddressLine2) {
            this.applicantAddressLine2 = applicantAddressLine2;
            return this;
        }

        public Builder applicantAddressLine3(String applicantAddressLine3) {
            this.applicantAddressLine3 = applicantAddressLine3;
            return this;
        }

        public Builder applicantAddressLine4(String applicantAddressLine4) {
            this.applicantAddressLine4 = applicantAddressLine4;
            return this;
        }

        public Builder applicantAddressLine5(String applicantAddressLine5) {
            this.applicantAddressLine5 = applicantAddressLine5;
            return this;
        }

        public Builder applicantPostcode(String applicantPostcode) {
            this.applicantPostcode = applicantPostcode;
            return this;
        }

        public Builder applicantPhone(String applicantPhone) {
            this.applicantPhone = applicantPhone;
            return this;
        }

        public Builder applicantMobile(String applicantMobile) {
            this.applicantMobile = applicantMobile;
            return this;
        }

        public Builder applicantEmail(String applicantEmail) {
            this.applicantEmail = applicantEmail;
            return this;
        }

        public Builder applicantName(String applicantName) {
            this.applicantName = applicantName;
            return this;
        }

        public Builder respondentTitle(String respondentTitle) {
            this.respondentTitle = respondentTitle;
            return this;
        }

        public Builder respondentSurname(String respondentSurname) {
            this.respondentSurname = respondentSurname;
            return this;
        }

        public Builder respondentForename1(String respondentForename1) {
            this.respondentForename1 = respondentForename1;
            return this;
        }

        public Builder respondentForename2(String respondentForename2) {
            this.respondentForename2 = respondentForename2;
            return this;
        }

        public Builder respondentForename3(String respondentForename3) {
            this.respondentForename3 = respondentForename3;
            return this;
        }

        public Builder respondentAddressLine1(String respondentAddressLine1) {
            this.respondentAddressLine1 = respondentAddressLine1;
            return this;
        }

        public Builder respondentAddressLine2(String respondentAddressLine2) {
            this.respondentAddressLine2 = respondentAddressLine2;
            return this;
        }

        public Builder respondentAddressLine3(String respondentAddressLine3) {
            this.respondentAddressLine3 = respondentAddressLine3;
            return this;
        }

        public Builder respondentAddressLine4(String respondentAddressLine4) {
            this.respondentAddressLine4 = respondentAddressLine4;
            return this;
        }

        public Builder respondentAddressLine5(String respondentAddressLine5) {
            this.respondentAddressLine5 = respondentAddressLine5;
            return this;
        }

        public Builder respondentPostcode(String respondentPostcode) {
            this.respondentPostcode = respondentPostcode;
            return this;
        }

        public Builder respondentPhone(String respondentPhone) {
            this.respondentPhone = respondentPhone;
            return this;
        }

        public Builder respondentMobile(String respondentMobile) {
            this.respondentMobile = respondentMobile;
            return this;
        }

        public Builder respondentEmail(String respondentEmail) {
            this.respondentEmail = respondentEmail;
            return this;
        }

        public Builder respondentDateOfBirth(OffsetDateTime respondentDateOfBirth) {
            this.respondentDateOfBirth = respondentDateOfBirth;
            return this;
        }

        public Builder respondentName(String respondentName) {
            this.respondentName = respondentName;
            return this;
        }

        public Builder applicationCode(String applicationCode) {
            this.applicationCode = applicationCode;
            return this;
        }

        public Builder applicationTitle(String applicationTitle) {
            this.applicationTitle = applicationTitle;
            return this;
        }

        public Builder applicationWording(String applicationWording) {
            this.applicationWording = applicationWording;
            return this;
        }

        public Builder caseReference(String caseReference) {
            this.caseReference = caseReference;
            return this;
        }

        public Builder accountReference(String accountReference) {
            this.accountReference = accountReference;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public ApplicationListEntryPrintProjection build() {
            return new Impl(
                    id,
                    sequenceNumber,
                    applicantTitle,
                    applicantSurname,
                    applicantForename1,
                    applicantForename2,
                    applicantForename3,
                    applicantAddressLine1,
                    applicantAddressLine2,
                    applicantAddressLine3,
                    applicantAddressLine4,
                    applicantAddressLine5,
                    applicantPostcode,
                    applicantPhone,
                    applicantMobile,
                    applicantEmail,
                    applicantName,
                    respondentTitle,
                    respondentSurname,
                    respondentForename1,
                    respondentForename2,
                    respondentForename3,
                    respondentAddressLine1,
                    respondentAddressLine2,
                    respondentAddressLine3,
                    respondentAddressLine4,
                    respondentAddressLine5,
                    respondentPostcode,
                    respondentPhone,
                    respondentMobile,
                    respondentEmail,
                    respondentDateOfBirth,
                    respondentName,
                    applicationCode,
                    applicationTitle,
                    applicationWording,
                    caseReference,
                    accountReference,
                    notes);
        }
    }

    private static final class Impl implements ApplicationListEntryPrintProjection {
        private final Long id;
        private final short sequenceNumber;
        private final String applicantTitle;
        private final String applicantSurname;
        private final String applicantForename1;
        private final String applicantForename2;
        private final String applicantForename3;
        private final String applicantAddressLine1;
        private final String applicantAddressLine2;
        private final String applicantAddressLine3;
        private final String applicantAddressLine4;
        private final String applicantAddressLine5;
        private final String applicantPostcode;
        private final String applicantPhone;
        private final String applicantMobile;
        private final String applicantEmail;
        private final String applicantName;
        private final String respondentTitle;
        private final String respondentSurname;
        private final String respondentForename1;
        private final String respondentForename2;
        private final String respondentForename3;
        private final String respondentAddressLine1;
        private final String respondentAddressLine2;
        private final String respondentAddressLine3;
        private final String respondentAddressLine4;
        private final String respondentAddressLine5;
        private final String respondentPostcode;
        private final String respondentPhone;
        private final String respondentMobile;
        private final String respondentEmail;
        private final OffsetDateTime respondentDateOfBirth;
        private final String respondentName;
        private final String applicationCode;
        private final String applicationTitle;
        private final String applicationWording;
        private final String caseReference;
        private final String accountReference;
        private final String notes;

        Impl(
                Long id,
                short sequenceNumber,
                String applicantTitle,
                String applicantSurname,
                String applicantForename1,
                String applicantForename2,
                String applicantForename3,
                String applicantAddressLine1,
                String applicantAddressLine2,
                String applicantAddressLine3,
                String applicantAddressLine4,
                String applicantAddressLine5,
                String applicantPostcode,
                String applicantPhone,
                String applicantMobile,
                String applicantEmail,
                String applicantName,
                String respondentTitle,
                String respondentSurname,
                String respondentForename1,
                String respondentForename2,
                String respondentForename3,
                String respondentAddressLine1,
                String respondentAddressLine2,
                String respondentAddressLine3,
                String respondentAddressLine4,
                String respondentAddressLine5,
                String respondentPostcode,
                String respondentPhone,
                String respondentMobile,
                String respondentEmail,
                OffsetDateTime respondentDateOfBirth,
                String respondentName,
                String applicationCode,
                String applicationTitle,
                String applicationWording,
                String caseReference,
                String accountReference,
                String notes) {
            this.id = id;
            this.sequenceNumber = sequenceNumber;
            this.applicantTitle = applicantTitle;
            this.applicantSurname = applicantSurname;
            this.applicantForename1 = applicantForename1;
            this.applicantForename2 = applicantForename2;
            this.applicantForename3 = applicantForename3;
            this.applicantAddressLine1 = applicantAddressLine1;
            this.applicantAddressLine2 = applicantAddressLine2;
            this.applicantAddressLine3 = applicantAddressLine3;
            this.applicantAddressLine4 = applicantAddressLine4;
            this.applicantAddressLine5 = applicantAddressLine5;
            this.applicantPostcode = applicantPostcode;
            this.applicantPhone = applicantPhone;
            this.applicantMobile = applicantMobile;
            this.applicantEmail = applicantEmail;
            this.applicantName = applicantName;
            this.respondentTitle = respondentTitle;
            this.respondentSurname = respondentSurname;
            this.respondentForename1 = respondentForename1;
            this.respondentForename2 = respondentForename2;
            this.respondentForename3 = respondentForename3;
            this.respondentAddressLine1 = respondentAddressLine1;
            this.respondentAddressLine2 = respondentAddressLine2;
            this.respondentAddressLine3 = respondentAddressLine3;
            this.respondentAddressLine4 = respondentAddressLine4;
            this.respondentAddressLine5 = respondentAddressLine5;
            this.respondentPostcode = respondentPostcode;
            this.respondentPhone = respondentPhone;
            this.respondentMobile = respondentMobile;
            this.respondentEmail = respondentEmail;
            this.respondentDateOfBirth = respondentDateOfBirth;
            this.respondentName = respondentName;
            this.applicationCode = applicationCode;
            this.applicationTitle = applicationTitle;
            this.applicationWording = applicationWording;
            this.caseReference = caseReference;
            this.accountReference = accountReference;
            this.notes = notes;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public short getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public String getApplicantTitle() {
            return applicantTitle;
        }

        @Override
        public String getApplicantSurname() {
            return applicantSurname;
        }

        @Override
        public String getApplicantForename1() {
            return applicantForename1;
        }

        @Override
        public String getApplicantForename2() {
            return applicantForename2;
        }

        @Override
        public String getApplicantForename3() {
            return applicantForename3;
        }

        @Override
        public String getApplicantAddressLine1() {
            return applicantAddressLine1;
        }

        @Override
        public String getApplicantAddressLine2() {
            return applicantAddressLine2;
        }

        @Override
        public String getApplicantAddressLine3() {
            return applicantAddressLine3;
        }

        @Override
        public String getApplicantAddressLine4() {
            return applicantAddressLine4;
        }

        @Override
        public String getApplicantAddressLine5() {
            return applicantAddressLine5;
        }

        @Override
        public String getApplicantPostcode() {
            return applicantPostcode;
        }

        @Override
        public String getApplicantPhone() {
            return applicantPhone;
        }

        @Override
        public String getApplicantMobile() {
            return applicantMobile;
        }

        @Override
        public String getApplicantEmail() {
            return applicantEmail;
        }

        @Override
        public String getApplicantName() {
            return applicantName;
        }

        @Override
        public String getRespondentTitle() {
            return respondentTitle;
        }

        @Override
        public String getRespondentSurname() {
            return respondentSurname;
        }

        @Override
        public String getRespondentForename1() {
            return respondentForename1;
        }

        @Override
        public String getRespondentForename2() {
            return respondentForename2;
        }

        @Override
        public String getRespondentForename3() {
            return respondentForename3;
        }

        @Override
        public String getRespondentAddressLine1() {
            return respondentAddressLine1;
        }

        @Override
        public String getRespondentAddressLine2() {
            return respondentAddressLine2;
        }

        @Override
        public String getRespondentAddressLine3() {
            return respondentAddressLine3;
        }

        @Override
        public String getRespondentAddressLine4() {
            return respondentAddressLine4;
        }

        @Override
        public String getRespondentAddressLine5() {
            return respondentAddressLine5;
        }

        @Override
        public String getRespondentPostcode() {
            return respondentPostcode;
        }

        @Override
        public String getRespondentPhone() {
            return respondentPhone;
        }

        @Override
        public String getRespondentMobile() {
            return respondentMobile;
        }

        @Override
        public String getRespondentEmail() {
            return respondentEmail;
        }

        @Override
        public OffsetDateTime getRespondentDateOfBirth() {
            return respondentDateOfBirth;
        }

        @Override
        public String getRespondentName() {
            return respondentName;
        }

        @Override
        public String getApplicationCode() {
            return applicationCode;
        }

        @Override
        public String getApplicationTitle() {
            return applicationTitle;
        }

        @Override
        public String getApplicationWording() {
            return applicationWording;
        }

        @Override
        public String getCaseReference() {
            return caseReference;
        }

        @Override
        public String getAccountReference() {
            return accountReference;
        }

        @Override
        public String getNotes() {
            return notes;
        }
    }
}
