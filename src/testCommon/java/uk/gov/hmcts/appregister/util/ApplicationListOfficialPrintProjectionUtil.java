package uk.gov.hmcts.appregister.util;

import uk.gov.hmcts.appregister.common.projection.ApplicationListOfficialPrintProjection;

public final class ApplicationListOfficialPrintProjectionUtil {

    public static Builder applicationListOfficialPrintProjection() {
        return new Builder();
    }

    public static final class Builder {
        private String type;
        private String title;
        private String forename;
        private String surname;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder forename(String forename) {
            this.forename = forename;
            return this;
        }

        public Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public ApplicationListOfficialPrintProjection build() {
            return new Impl(
                    type,
                    title,
                    forename,
                    surname);
        }
    }

    private static final class Impl implements ApplicationListOfficialPrintProjection {
        private final String type;
        private final String title;
        private final String forename;
        private final String surname;

        Impl(
                String type,
                String title,
                String forename,
                String surname) {
            this.type = type;
            this.title = title;
            this.forename = forename;
            this.surname = surname;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getForename() { return forename; }

        @Override
        public String getSurname() {
            return surname;
        }
    }
}
