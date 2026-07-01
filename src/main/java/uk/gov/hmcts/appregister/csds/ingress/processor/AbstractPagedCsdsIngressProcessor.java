package uk.gov.hmcts.appregister.csds.ingress.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressClient;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPagedCsdsIngressProcessor<T, DiffT>
        implements IDataIngressProcessor<T> {
    private static final String DATA_LOCATION_NAME = "CSDS";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VIEW_TYPE = "GD";

    private final CsdsIngressProperties properties;
    private final String sourceEntityName;

    @Override
    public final List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        val mockFilePath = mockFilePath();
        if (StringUtils.hasText(mockFilePath)) {
            return List.of(loadMockResponse(mockFilePath));
        }

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
                            appendPagingParameters(
                                    appendQueryParameters(queryPath(), queryParameters()),
                                    "%24limit="
                                            + properties.getPageSize()
                                            + "&%24offset="
                                            + offset)));
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

    protected String queryParameters() {
        return null;
    }

    protected String mockFilePath() {
        return null;
    }

    protected final String appendQueryParameters(String path, String parameters) {
        if (!StringUtils.hasText(parameters)) {
            return path;
        }

        return path + parameters;
    }

    protected final Long requiredLong(JsonNode node, String fieldName) {
        val value = nullableLong(node, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        return value;
    }

    protected final Long nullableLong(JsonNode node, String fieldName) {
        val field = node.get(fieldName);
        if (field == null || !field.canConvertToLong()) {
            return null;
        }

        return field.longValue();
    }

    protected final String requiredText(JsonNode node, String fieldName) {
        val value = nullableText(node, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        return value;
    }

    protected final String nullableText(JsonNode node, String fieldName) {
        val field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }

        return field.asText();
    }

    protected final YesOrNo requiredYesOrNo(JsonNode node, String fieldName) {
        val value = nullableText(node, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        try {
            return YesOrNo.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field "
                            + fieldName
                            + " contained an unknown YesOrNo value for "
                            + datasetName(),
                    ex);
        }
    }

    protected final LocalDate requiredLocalDate(JsonNode node, String fieldName) {
        val value = nullableLocalDate(node, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        return value;
    }

    protected final LocalDate nullableLocalDate(JsonNode node, String fieldName) {
        val value = nullableText(node, fieldName);
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field " + fieldName + " contained an invalid date for " + datasetName(),
                    ex);
        }
    }

    protected final AppRegistryException invalidField(String fieldName) {
        return new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "CSDS field " + fieldName + " was missing or invalid for " + datasetName());
    }

    @Override
    public final void apply(T processedData) {
        val diff = diff(processedData);
        report(processedData, diff);
        applyDiff(diff);
    }

    protected abstract DiffT diff(T processedData);

    protected void report(T processedData, DiffT diff) {
        // Reporting is optional per processor and can be implemented when configured.
    }

    protected void applyDiff(DiffT diff) {
        // Database apply is optional until concrete update flows are introduced.
    }

    private String appendPagingParameters(String path, String pagingParameters) {
        return path + (path.contains("?") ? "&" : "?") + pagingParameters;
    }

    private JsonNode loadMockResponse(String mockFilePath) {
        try {
            val resourcePath =
                    mockFilePath.startsWith("classpath:")
                            ? mockFilePath.substring(10)
                            : mockFilePath;
            val resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                try (val inputStream = resource.getInputStream()) {
                    val response = OBJECT_MAPPER.readTree(inputStream);
                    log.info(
                            "Loaded CSDS mock response for {} from classpath:{}",
                            datasetName(),
                            resourcePath);
                    return response;
                }
            }

            val response = OBJECT_MAPPER.readTree(Files.readString(Path.of(mockFilePath)));
            log.info("Loaded CSDS mock response for {} from {}", datasetName(), mockFilePath);
            return response;
        } catch (IOException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Failed to load CSDS mock response for "
                            + datasetName()
                            + " from "
                            + mockFilePath,
                    ex);
        }
    }
}
