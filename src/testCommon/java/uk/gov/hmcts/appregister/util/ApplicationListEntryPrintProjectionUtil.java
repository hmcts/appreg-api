package uk.gov.hmcts.appregister.util;

import java.time.LocalDate;
import java.util.UUID;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryPrintProjection;

public final class ApplicationListEntryPrintProjectionUtil {
    private ApplicationListEntryPrintProjectionUtil() {
        /* This utility class should not be instantiated */
    }

    public static Builder applicationListEntryPrintProjection() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private UUID uuid;
        private short sequenceNumber;
        private String applicantTitle;
        private String applicantLastName;
        private String applicantFirstName;
        private String applicantMiddleName;
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
        private String respondentLastName;
        private String respondentFirstName;
        private String respondentMiddleName;
        private String respondentAddressLine1;
        private String respondentAddressLine2;
        private String respondentAddressLine3;
        private String respondentAddressLine4;
        private String respondentAddressLine5;
        private String respondentPostcode;
        private String respondentPhone;
        private String respondentMobile;
        private String respondentEmail;
        private LocalDate respondentDateOfBirth;
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

        public Builder applicantLastName(String applicantLastName) {
            this.applicantLastName = applicantLastName;
            return this;
        }

        public Builder applicantFirstName(String applicantFirstName) {
            this.applicantFirstName = applicantFirstName;
            return this;
        }

        public Builder applicantMiddleName(String applicantMiddleName) {
            this.applicantMiddleName = applicantMiddleName;
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

        public Builder respondentLastName(String respondentLastName) {
            this.respondentLastName = respondentLastName;
            return this;
        }

        public Builder respondentFirstName(String respondentFirstName) {
            this.respondentFirstName = respondentFirstName;
            return this;
        }

        public Builder respondentMiddleName(String respondentMiddleName) {
            this.respondentMiddleName = respondentMiddleName;
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

        public Builder respondentDateOfBirth(LocalDate respondentDateOfBirth) {
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
                    uuid,
                    sequenceNumber,
                    applicantTitle,
                    applicantLastName,
                    applicantFirstName,
                    applicantMiddleName,
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
                    respondentLastName,
                    respondentFirstName,
                    respondentMiddleName,
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
        private final UUID uuid;
        private final short sequenceNumber;
        private final String applicantTitle;
        private final String applicantLastName;
        private final String applicantFirstName;
        private final String applicantMiddleName;
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
        private final String respondentLastName;
        private final String respondentFirstName;
        private final String respondentMiddleName;
        private final String respondentAddressLine1;
        private final String respondentAddressLine2;
        private final String respondentAddressLine3;
        private final String respondentAddressLine4;
        private final String respondentAddressLine5;
        private final String respondentPostcode;
        private final String respondentPhone;
        private final String respondentMobile;
        private final String respondentEmail;
        private final LocalDate respondentDateOfBirth;
        private final String respondentName;
        private final String applicationCode;
        private final String applicationTitle;
        private final String applicationWording;
        private final String caseReference;
        private final String accountReference;
        private final String notes;

        Impl(
                Long id,
                UUID uuid,
                short sequenceNumber,
                String applicantTitle,
                String applicantLastName,
                String applicantFirstName,
                String applicantMiddleName,
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
                String respondentLastName,
                String respondentFirstName,
                String respondentMiddleName,
                String respondentAddressLine1,
                String respondentAddressLine2,
                String respondentAddressLine3,
                String respondentAddressLine4,
                String respondentAddressLine5,
                String respondentPostcode,
                String respondentPhone,
                String respondentMobile,
                String respondentEmail,
                LocalDate respondentDateOfBirth,
                String respondentName,
                String applicationCode,
                String applicationTitle,
                String applicationWording,
                String caseReference,
                String accountReference,
                String notes) {
            this.id = id;
            this.uuid = uuid;
            this.sequenceNumber = sequenceNumber;
            this.applicantTitle = applicantTitle;
            this.applicantLastName = applicantLastName;
            this.applicantFirstName = applicantFirstName;
            this.applicantMiddleName = applicantMiddleName;
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
            this.respondentLastName = respondentLastName;
            this.respondentFirstName = respondentFirstName;
            this.respondentMiddleName = respondentMiddleName;
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
        public UUID getUuid() {
            return uuid;
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
        public String getApplicantLastName() {
            return applicantLastName;
        }

        @Override
        public String getApplicantFirstName() {
            return applicantFirstName;
        }

        @Override
        public String getApplicantMiddleName() {
            return applicantMiddleName;
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
        public String getRespondentLastName() {
            return respondentLastName;
        }

        @Override
        public String getRespondentFirstName() {
            return respondentFirstName;
        }

        @Override
        public String getRespondentMiddleName() {
            return respondentMiddleName;
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
        public LocalDate getRespondentDateOfBirth() {
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
