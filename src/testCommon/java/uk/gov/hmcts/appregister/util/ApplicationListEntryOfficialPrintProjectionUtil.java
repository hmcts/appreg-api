package uk.gov.hmcts.appregister.util;

import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;

public final class ApplicationListEntryOfficialPrintProjectionUtil {

    public static Builder applicationListEntryOfficialPrintProjection() {
        return new Builder();
    }

    public static final class Builder {
        private Long entryId;
        private String type;
        private String title;
        private String forename;
        private String surname;

        public Builder entryId(Long entryId) {
            this.entryId = entryId;
            return this;
        }

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

        public ApplicationListEntryOfficialPrintProjection build() {
            return new Impl(entryId, type, title, forename, surname);
        }
    }

    private static final class Impl implements ApplicationListEntryOfficialPrintProjection {
        private final Long entryId;
        private final String type;
        private final String title;
        private final String forename;
        private final String surname;

        Impl(Long entryId, String type, String title, String forename, String surname) {
            this.entryId = entryId;
            this.type = type;
            this.title = title;
            this.forename = forename;
            this.surname = surname;
        }

        @Override
        public Long getEntryId() {
            return entryId;
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
        public String getForename() {
            return forename;
        }

        @Override
        public String getSurname() {
            return surname;
        }
    }
}
