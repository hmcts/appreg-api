package uk.gov.hmcts.appregister.csds.ingress.processor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

public interface IDataIngressProcessor<T> {
    String processorName();

    default boolean enabled() {
        return true;
    }

    String targetTable();

    String targetKeyField();

    default String datasetName() {
        return processorName();
    }

    default List<String> sourcePaths() {
        return List.of();
    }

    default List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        return sourcePaths().stream().map(ingressClient::retrieveJson).toList();
    }

    default void backup() {
        // Optional hook for processors that want to snapshot a source table before apply.
    }

    T preProcess(List<JsonNode> rawJson);

    void apply(T processedData);

    CsdsIngestResponse ingest(List<JsonNode> rawJson);
}
