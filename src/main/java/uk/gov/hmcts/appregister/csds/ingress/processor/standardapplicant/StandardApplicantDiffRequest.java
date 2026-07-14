package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Function;

public record StandardApplicantDiffRequest(
        String targetTable,
        List<JsonNode> processedData,
        Function<JsonNode, StandardApplicantIngressRecord> recordMapper,
        Function<JsonNode, List<JsonNode>> recordsExtractor) {}
