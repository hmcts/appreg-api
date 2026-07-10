package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Function;

public record ApplicationCodeDiffRequest(
        String targetTable,
        List<JsonNode> processedData,
        Function<JsonNode, ApplicationCodeIngressRecord> recordMapper,
        Function<JsonNode, List<JsonNode>> recordsExtractor) {}
