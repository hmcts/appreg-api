package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Function;

public record ResolutionCodeDiffRequest(
        String targetTable,
        List<JsonNode> processedData,
        Function<JsonNode, ResolutionCodeIngressRecord> recordMapper,
        Function<JsonNode, List<JsonNode>> recordsExtractor) {}
