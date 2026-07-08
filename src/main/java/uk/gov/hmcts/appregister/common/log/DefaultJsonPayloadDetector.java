package uk.gov.hmcts.appregister.common.log;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Default JSON payload detector based on media type.
 */
@Component
public class DefaultJsonPayloadDetector implements JsonPayloadDetector {
    @Override
    public boolean isJson(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }

        String subtype = mediaType.getSubtype();
        if (subtype.isBlank()) {
            return false;
        }

        return MediaType.APPLICATION_JSON.includes(mediaType)
                || subtype.endsWith("+json")
                || subtype.contains("json");
    }
}
