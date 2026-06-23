package uk.gov.hmcts.appregister.common.api;

import org.springframework.http.MediaType;

public final class ApiConstants {
    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final class MediaTypes {
        private MediaTypes() {
            throw new UnsupportedOperationException("Utility class");
        }

        public static final MediaType VND_JSON_V1 =
                MediaType.parseMediaType("application/vnd.hmcts.appreg.v1+json");
        public static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");
    }
}
