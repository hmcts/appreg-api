package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Function;

public record FeeDiffRequest(
        String targetTable,
        List<JsonNode> processedData,
        Function<JsonNode, FeeIngressRecord> recordMapper,
        Function<JsonNode, List<JsonNode>> recordsExtractor) {}
