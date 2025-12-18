package uk.gov.hmcts.appregister.common.concurrency;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Represents a match provider that allows us to obtain the match information from the active
 * request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpMatchProviderImpl implements MatchProvider {
    private final HttpServletRequest request;

    public String getEtag() {
        try {
            return request.getHeader(HttpHeaders.IF_MATCH);
        } catch (IllegalStateException e) {
            log.error("No current HTTP request available to obtain ETag", e);
        }
        return null;
    }
}
