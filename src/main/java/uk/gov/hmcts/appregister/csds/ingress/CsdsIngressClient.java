package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;

public interface CsdsIngressClient {
    JsonNode retrieveJson(String path);
}
