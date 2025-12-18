package uk.gov.hmcts.appregister.util;

import uk.gov.hmcts.appregister.common.enumeration.OfficialType;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryOfficialPrintProjection;

public final class ApplicationListEntryOfficialPrintProjectionUtil {

    public static Builder applicationListEntryOfficialPrintProjection() {
        return new Builder();
    }

    public static final class Builder {
        private Long entryId;
        private OfficialType type;
        private String title;
        private String forename;
        private String surname;

        public Builder entryId(Long entryId) {
            this.entryId = entryId;
            return this;
        }

        public Builder type(OfficialType type) {
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
        private final OfficialType type;
        private final String title;
        private final String forename;
        private final String surname;

        Impl(Long entryId, OfficialType type, String title, String forename, String surname) {
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
        public OfficialType getType() {
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
