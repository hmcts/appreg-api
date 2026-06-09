package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractPagedCsdsIngressProcessor<T> implements IDataIngressProcessor<T> {
    private static final String DATA_LOCATION_NAME = "CSDS";
    private static final String VIEW_TYPE = "GD";

    private final CsdsIngressProperties properties;
    private final String sourceEntityName;

    @Override
    public final List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        val totalCount = extractCount(ingressClient.retrieveJson(countPath()));

        if (totalCount == 0) {
            log.info(
                    "No CSDS records reported for {} using target {}.{}",
                    datasetName(),
                    targetTable(),
                    targetKeyField());
            return List.of();
        }

        val responses = new ArrayList<JsonNode>();
        for (var offset = 0; offset < totalCount; offset += properties.getPageSize()) {
            responses.add(
                    ingressClient.retrieveJson(
                            queryPath()
                                    + "?%24limit="
                                    + properties.getPageSize()
                                    + "&%24offset="
                                    + offset));
        }

        log.info(
                "Retrieved {} CSDS pages for {} using page size {} and reported count {}",
                responses.size(),
                datasetName(),
                properties.getPageSize(),
                totalCount);

        return List.copyOf(responses);
    }

    protected final List<JsonNode> extractRecords(JsonNode response) {
        val recordsNode = response.get("records");
        if (recordsNode == null || !recordsNode.isArray()) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS query response did not contain a records array for " + datasetName());
        }

        val records = new ArrayList<JsonNode>();
        recordsNode.forEach(records::add);
        return List.copyOf(records);
    }

    private int extractCount(JsonNode response) {
        val countNode = response.get("count");
        if (countNode == null || !countNode.canConvertToInt()) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS count response did not contain a numeric count for " + datasetName());
        }

        return countNode.intValue();
    }

    private String countPath() {
        return "/count/" + DATA_LOCATION_NAME + "/" + sourceEntityName + "/" + VIEW_TYPE;
    }

    private String queryPath() {
        return "/query/" + DATA_LOCATION_NAME + "/" + sourceEntityName + "/" + VIEW_TYPE;
    }
}
