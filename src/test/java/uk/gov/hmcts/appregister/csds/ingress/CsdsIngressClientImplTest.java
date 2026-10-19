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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

        client.retrieveJson("/query/APPREGISTER/ApplicationCode/GD?%24limit=100&%24offset=200");

        var uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString())
                .isEqualTo(
                        "https://csds.dev.apps.hmcts.net/api/rest/query/APPREGISTER/ApplicationCode/GD"
                                + "?%24limit=100&%24offset=200");
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "{\"count\":0}|true",
                "{\"records\":[]}|true",
                "{\"count\":2}|false",
                "{\"records\":[{}]}|false",
                "{}|false",
                "{\"count\":\"invalid\"}|false",
                "{\"records\":null}|false"
            },
            delimiter = '|')
    @SuppressWarnings("unchecked")
    void given_response_when_retrieveJson_then_warnsOnlyForNoRecords(String payload, boolean warns)
            throws Exception {
        var restClient = mock(RestClient.class);
        var request = mock(RestClient.RequestHeadersUriSpec.class);
        var headers = mock(RestClient.RequestHeadersSpec.class);
        var response = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(request);
        when(request.uri(any(URI.class))).thenReturn(headers);
        when(headers.header("Api-Key", "test-key")).thenReturn(headers);
        when(headers.accept(MediaType.APPLICATION_JSON)).thenReturn(headers);
        when(headers.retrieve()).thenReturn(response);
        when(response.body(String.class)).thenReturn(payload);
        var properties = new CsdsIngressProperties();
        properties.setBaseUrl("https://example.test/api/rest/");
        properties.setAccessKeys(List.of("test-key"));
        var path = "query/COURT/Court/GD?%24f=PublishingStatus='Active'&%24limit=100&%24offset=0";

        try (var logs = LogCaptor.forClass(CsdsIngressClientImpl.class)) {
            logs.clearLogs();
            var actual = new CsdsIngressClientImpl(restClient, properties).retrieveJson(path);
            assertThat(actual).isEqualTo(OBJECT_MAPPER.readTree(payload));
            assertThat(logs.getWarnLogs())
                    .containsExactlyElementsOf(
                            warns
                                    ? List.of(
                                            "NO RECORDS RETURNED for query https://example.test/api/rest/"
                                                    + path)
                                    : List.of());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void given_multipleAccessKeysFail_when_retrieveJson_then_warningLogsNameEachKey() {
        var restClient = mock(RestClient.class);
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        var logCaptor = LogCaptor.forClass(CsdsIngressClientImpl.class);
        logCaptor.clearLogs();

        var responseSpec = mock(RestClient.ResponseSpec.class);
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

        assertThatThrownBy(() -> client.retrieveJson("/count/APPREGISTER/ApplicationCode/GD"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve CSDS data for path");
        assertThat(logCaptor.getWarnLogs())
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to retrieve CSDS JSON, using Key 1, for path "
                                                + "/count/APPREGISTER/ApplicationCode/GD: 404 Not Found"))
                .anyMatch(
                        log ->
                                log.contains(
                                        "Failed to retrieve CSDS JSON, using Key 2, for path "
                                                + "/count/APPREGISTER/ApplicationCode/GD: 404 Not Found"));
    }
}
