package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class CsdsIngressClientImplTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void given_percentEncodedPagingParameters_when_retrieveJson_then_preservesThemInRequestUri() {
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
        properties.setAccessKeys(java.util.List.of("test-key"));

        var client = new CsdsIngressClientImpl(restClient, properties);

        client.retrieveJson("/query/CSDS/ApplicationCode/GD?%24limit=100&%24offset=200");

        var uriCaptor = ArgumentCaptor.forClass(URI.class);
        org.mockito.Mockito.verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString())
                .isEqualTo(
                        "https://csds.dev.apps.hmcts.net/api/rest/query/CSDS/ApplicationCode/GD?%24limit=100&%24offset=200");
    }
}
