package uk.gov.hmcts.appregister.util;

import uk.gov.hmcts.appregister.common.projection.ApplicationListEntrySummaryProjection;

public final class ApplicationListEntrySummaryProjectionUtil {

    public static Builder applicationListEntrySummaryProjection() {
        return new Builder();
    }

    public static final class Builder {
        private short sequenceNumber;
        private String accountNumber;
        private String applicant;
        private String respondent;
        private String postCode;
        private String applicationTitle;
        private boolean feeRequired;
        private String result;

        public Builder sequenceNumber(int sequenceNumber) {
            this.sequenceNumber = (short) sequenceNumber;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder applicant(String applicant) {
            this.applicant = applicant;
            return this;
        }

        public Builder respondent(String respondent) {
            this.respondent = respondent;
            return this;
        }

        public Builder postCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Builder applicationTitle(String applicationTitle) {
            this.applicationTitle = applicationTitle;
            return this;
        }

        public Builder feeRequired(boolean feeRequired) {
            this.feeRequired = feeRequired;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
            return this;
        }

        public ApplicationListEntrySummaryProjection build() {
            return new Impl(
                    sequenceNumber,
                    accountNumber,
                    applicant,
                    respondent,
                    postCode,
                    applicationTitle,
                    feeRequired,
                    result);
        }
    }

    private static final class Impl implements ApplicationListEntrySummaryProjection {
        private final short sequenceNumber;
        private final String accountNumber;
        private final String applicant;
        private final String respondent;
        private final String postCode;
        private final String applicationTitle;
        private final boolean feeRequired;
        private final String result;

        Impl(
                short sequenceNumber,
                String accountNumber,
                String applicant,
                String respondent,
                String postCode,
                String applicationTitle,
                boolean feeRequired,
                String result) {
            this.sequenceNumber = sequenceNumber;
            this.accountNumber = accountNumber;
            this.applicant = applicant;
            this.respondent = respondent;
            this.postCode = postCode;
            this.applicationTitle = applicationTitle;
            this.feeRequired = feeRequired;
            this.result = result;
        }

        @Override
        public short getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public String getAccountNumber() {
            return accountNumber;
        }

        @Override
        public String getApplicant() {
            return applicant;
        }

        @Override
        public String getRespondent() {
            return respondent;
        }

        @Override
        public String getPostCode() {
            return postCode;
        }

        @Override
        public String getApplicationTitle() {
            return applicationTitle;
        }

        @Override
        public boolean isFeeRequired() {
            return feeRequired;
        }

        @Override
        public String getResult() {
            return result;
        }
    }
}
