package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface IDataIngressProcessor<T> {
    String datasetName();

    List<String> sourcePaths();

    default List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        return sourcePaths().stream().map(ingressClient::retrieveJson).toList();
    }

    T preProcess(List<JsonNode> rawJson);

    void handle(T processedData);
}
