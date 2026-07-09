package uk.gov.hmcts.appregister.csds.ingress.processor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;

public interface IDataIngressProcessor<T> {
    String targetTable();

    String targetKeyField();

    default String datasetName() {
        return targetTable();
    }

    default List<String> sourcePaths() {
        return List.of();
    }

    default List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        return sourcePaths().stream().map(ingressClient::retrieveJson).toList();
    }

    T preProcess(List<JsonNode> rawJson);

    void apply(T processedData);
}
