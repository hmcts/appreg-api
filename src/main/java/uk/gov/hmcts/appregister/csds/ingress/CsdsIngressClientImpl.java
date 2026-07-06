package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "appreg.csds.ingress", name = "enabled", havingValue = "true")
class CsdsIngressClientImpl implements CsdsIngressClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient csdsIngressRestClient;
    private final CsdsIngressProperties properties;

    @Override
    public JsonNode retrieveJson(String path) {
        validatePath(path);

        RestClientException lastException = null;

        for (val accessKey : properties.getAccessKeys()) {
            try {
                val response =
                        csdsIngressRestClient
                                .get()
                                .uri(buildUri(path))
                                .header(properties.getAccessKeyHeader(), accessKey)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(String.class);

                if (response == null) {
                    throw new AppRegistryException(
                            CommonAppError.INTERNAL_SERVER_ERROR,
                            "CSDS response body was null for path " + path);
                }

                return readJson(response, path);
            } catch (RestClientException ex) {
                lastException = ex;
                log.warn(
                        "Failed to retrieve CSDS JSON for path {} using one configured access key: {}",
                        path,
                        ex.getMessage());
            }
        }

        throw new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "Failed to retrieve CSDS data for path " + path,
                lastException);
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private JsonNode readJson(String responseBody, String path) {
        try {
            return OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Failed to parse CSDS JSON for path " + path,
                    ex);
        }
    }

    private URI buildUri(String path) {
        return URI.create(properties.getBaseUrl() + normalizePath(path));
    }

    private static void validatePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("CSDS path must not be blank");
        }
    }
}
