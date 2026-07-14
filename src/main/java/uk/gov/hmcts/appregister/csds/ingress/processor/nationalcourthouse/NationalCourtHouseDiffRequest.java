package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Function;

public record NationalCourtHouseDiffRequest(
        String targetTable,
        List<JsonNode> processedData,
        Function<JsonNode, NationalCourtHouseIngressRecord> recordMapper,
        Function<JsonNode, List<JsonNode>> recordsExtractor) {}
