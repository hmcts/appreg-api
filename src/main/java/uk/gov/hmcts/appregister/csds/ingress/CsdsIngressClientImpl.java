package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
class CsdsIngressClientImpl implements CsdsIngressClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient csdsIngressRestClient;
    private final CsdsIngressProperties properties;

    @Override
    public JsonNode retrieveJson(String path) {
        validatePath(path);

        RestClientException lastException = null;
        var accessKeys = properties.getAccessKeys();

        for (var index = 0; index < accessKeys.size(); index++) {
            val accessKey = accessKeys.get(index);
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
                        "Failed to retrieve CSDS JSON, using {}, for path {}: {}",
                        "Key " + (index + 1),
                        path,
                        ex.getMessage());
                log.debug("CSDS request failed for path {}", path, ex);
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
        // Build the full URI explicitly so a configured base URL with a path component like
        // /api/rest is preserved while already-escaped query fragments such as %24limit are left
        // untouched.
        return URI.create(normalizeBaseUrl(properties.getBaseUrl()) + normalizePath(path));
    }

    private static String normalizeBaseUrl(String baseUrl) {
        var normalized = baseUrl;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void validatePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("CSDS path must not be blank");
        }
    }
}
