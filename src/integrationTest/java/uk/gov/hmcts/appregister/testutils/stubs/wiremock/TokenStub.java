package uk.gov.hmcts.appregister.testutils.stubs.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenStub {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    protected String jwksUri;

    @SuppressWarnings("checkstyle:linelength")
    public void stubExternalJwksKeys(String keys) {
        String path = URI.create(jwksUri).getPath();

        stubFor(
                get(urlPathEqualTo(path))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                {"keys":[%s]}
                                """
                                                        .formatted(keys))));
    }

    static class UrlMatcher extends RequestMatcherExtension {
        private final String url;

        UrlMatcher(String url) {
            this.url = url;
        }

        @Override
        public MatchResult match(Request request, Parameters parameters) {
            return MatchResult.of(request.getAbsoluteUrl().equals(url));
        }
    }
}
