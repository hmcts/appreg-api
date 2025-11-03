package uk.gov.hmcts.appregister.common.concurrency;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Represents a match provider that allows us to obtain the match infomration from the active
 * request.
 */
@Component
@RequiredArgsConstructor
public class MatchProvider {
    private final HttpServletRequest request;

    public String getEtag() {
        return request.getHeader(HttpHeaders.IF_MATCH);
    }
}
