package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class CsdsIngressClientImplTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void
            given_baseUrlWithPathAndPercentEncodedPagingParameters_when_retrieveJson_then_preservesFullRequestUri() {
        var restClient = mock(RestClient.class);
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq("Api-Key"), eq("test-key")))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(String.class)))
                .thenReturn(OBJECT_MAPPER.createObjectNode().putArray("records").toString());

        var properties = new CsdsIngressProperties();
        properties.setBaseUrl("https://csds.dev.apps.hmcts.net/api/rest");
        properties.setAccessKeyHeader("Api-Key");
        properties.setAccessKeys(List.of("test-key"));

        var client = new CsdsIngressClientImpl(restClient, properties);

        client.retrieveJson("/query/CSDS/ApplicationCode/GD?%24limit=100&%24offset=200");

        var uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString())
                .isEqualTo(
                        "https://csds.dev.apps.hmcts.net/api/rest/query/CSDS/ApplicationCode/GD"
                                + "?%24limit=100&%24offset=200");
    }

    @Test
    @SuppressWarnings("unchecked")
    void given_multipleAccessKeysFail_when_retrieveJson_then_warningLogsNameEachKey() {
        var restClient = mock(RestClient.class);
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        var logCaptor = LogCaptor.forClass(CsdsIngressClientImpl.class);
        logCaptor.clearLogs();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq("Api-Key"), any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(String.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        var properties = new CsdsIngressProperties();
        properties.setBaseUrl("https://csds.dev.apps.hmcts.net/api/rest");
        properties.setAccessKeyHeader("Api-Key");
        properties.setAccessKeys(List.of("primary-key", "secondary-key"));

        var client = new CsdsIngressClientImpl(restClient, properties);

        assertThatThrownBy(() -> client.retrieveJson("/count/CSDS/ApplicationCode/GD"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve CSDS data for path");
        assertThat(logCaptor.getWarnLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to retrieve CSDS JSON, using Key 1, for path "
                                                + "/count/CSDS/ApplicationCode/GD: 404 Not Found"))
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to retrieve CSDS JSON, using Key 2, for path "
                                                + "/count/CSDS/ApplicationCode/GD: 404 Not Found"));
    }
}
