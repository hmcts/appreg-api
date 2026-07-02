package uk.gov.hmcts.appregister.common.log;

import org.springframework.http.MediaType;

/**
 * Determines whether a media type should be treated as a JSON payload for logging.
 */
public interface JsonPayloadDetector {
    boolean isJson(MediaType mediaType);
}
