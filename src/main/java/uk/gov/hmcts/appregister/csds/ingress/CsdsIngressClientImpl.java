package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;
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
                                .uri(normalizePath(path))
                                .header(properties.getAccessKeyHeader(), accessKey)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .body(JsonNode.class);

                if (response == null) {
                    throw new AppRegistryException(
                            CommonAppError.INTERNAL_SERVER_ERROR,
                            "CSDS response body was null for path " + path);
                }

                return response;
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

    private static void validatePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("CSDS path must not be blank");
        }
    }
}
