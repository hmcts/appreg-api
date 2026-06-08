package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.processors.dummy-logging",
        name = "enabled",
        havingValue = "true")
class DummyLoggingDataIngressProcessor implements IDataIngressProcessor<List<JsonNode>> {
    private static final String DATASET_NAME = "dummy-logging";
    private static final List<String> SOURCE_PATHS = List.of("/dummy/primary", "/dummy/secondary");

    @Override
    public String datasetName() {
        return DATASET_NAME;
    }

    @Override
    public List<String> sourcePaths() {
        return SOURCE_PATHS;
    }

    @Override
    public List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        log.info("Dummy CSDS ingress processor retrieve invoked for {}", DATASET_NAME);
        return IDataIngressProcessor.super.retrieve(ingressClient);
    }

    @Override
    public List<JsonNode> preProcess(List<JsonNode> rawJson) {
        log.info(
                "Dummy CSDS ingress processor preProcess invoked for {} with {} payloads",
                DATASET_NAME,
                rawJson.size());
        return rawJson;
    }

    @Override
    public void handle(List<JsonNode> processedData) {
        log.info(
                "Dummy CSDS ingress processor handle invoked for {} with {} payloads",
                DATASET_NAME,
                processedData.size());
    }
}
