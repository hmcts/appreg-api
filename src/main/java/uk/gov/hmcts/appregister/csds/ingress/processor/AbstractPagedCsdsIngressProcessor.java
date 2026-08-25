package uk.gov.hmcts.appregister.csds.ingress.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;
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
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditEntry;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.FailedUpsertRecord;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPagedCsdsIngressProcessor<D, R> implements IDataIngressProcessor<D> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VIEW_TYPE = "GD";

    private final CsdsIngressProperties properties;
    private final CsdsIngressProperties.ProcessorProperties processorProperties;
    private final CsdsAuditService csdsAuditService;
    private final CsdsIngressTransactionRunner csdsIngressTransactionRunner;
    private final JdbcIngressBackupService ingressBackupService;

    @Override
    public final boolean enabled() {
        return processorProperties.isEnabled();
    }

    @Override
    public final List<JsonNode> retrieve(CsdsIngressClient ingressClient) {
        val mockFilePath = mockFilePath();
        if (StringUtils.hasText(mockFilePath)) {
            val mockResponse = loadMockResponse(mockFilePath);
            if (mockResponse != null) {
                log.info(
                        "Loaded mock CSDS payload for {} with {} records",
                        datasetName(),
                        extractRecords(mockResponse).size());
                return List.of(mockResponse);
            }
        }

        val totalCount =
                extractCount(
                        ingressClient.retrieveJson(
                                appendQueryParameters(countPath(), queryParameters())));

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
        val fetchedRecordCount =
                responses.stream().mapToInt(response -> extractRecords(response).size()).sum();

        log.info(
                "Retrieved {} CSDS pages for {} using page size {} and reported count {} "
                        + "with actual fetched records {}",
                responses.size(),
                datasetName(),
                properties.getPageSize(),
                totalCount,
                fetchedRecordCount);

        return List.copyOf(responses);
    }

    @Override
    public final String targetTable() {
        return processorProperties.getIngressTarget();
    }

    @Override
    public final String targetKeyField() {
        return processorProperties.getPrimaryKey();
    }

    @Override
    public final void backup() {
        val backupSource = processorProperties.getBackupSource();
        val backupTarget = processorProperties.getBackupTarget();
        if (!StringUtils.hasText(backupSource)
                || !StringUtils.hasText(backupTarget)
                || backupSource.equals(backupTarget)) {
            return;
        }

        try {
            val result = ingressBackupService.backup(backupSource, backupTarget);
            log.info(
                    "Completed CSDS backup for {} from {} to {} with deleted={} and inserted={}",
                    datasetName(),
                    backupSource,
                    backupTarget,
                    result.deletedCount(),
                    result.insertedCount());
        } catch (RuntimeException ex) {
            log.error(
                    "Failed CSDS backup for {} from {} to {}. Continuing ingress.",
                    datasetName(),
                    backupSource,
                    backupTarget,
                    ex);
        }
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
        return "/"
                + countPathType()
                + "/"
                + dataLocationName()
                + "/"
                + processorProperties.getSourceEntityName()
                + "/"
                + VIEW_TYPE;
    }

    private String queryPath() {
        return "/"
                + queryPathType()
                + "/"
                + dataLocationName()
                + "/"
                + processorProperties.getSourceEntityName()
                + "/"
                + VIEW_TYPE;
    }

    protected String queryParameters() {
        return null;
    }

    protected abstract String dataLocationName();

    protected String queryPathType() {
        return "query";
    }

    protected String countPathType() {
        return "count";
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
            throw invalidField(node, fieldName);
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
            throw invalidField(node, fieldName);
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

    protected final BigDecimal requiredBigDecimal(JsonNode node, String fieldName) {
        val value = nullableBigDecimal(node, fieldName);
        if (value == null) {
            throw invalidField(node, fieldName);
        }

        return value;
    }

    protected final BigDecimal nullableBigDecimal(JsonNode node, String fieldName) {
        val field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }

        try {
            if (field.isNumber()) {
                return field.decimalValue();
            }
            if (field.isTextual() && StringUtils.hasText(field.asText())) {
                return new BigDecimal(field.asText());
            }
            return null;
        } catch (NumberFormatException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field "
                            + fieldName
                            + " contained an invalid decimal value for "
                            + datasetName()
                            + ". Record: "
                            + node,
                    ex);
        }
    }

    protected final YesOrNo requiredYesOrNo(JsonNode node, String fieldName) {
        val value = nullableText(node, fieldName);
        if (value == null) {
            throw invalidField(node, fieldName);
        }

        try {
            return YesOrNo.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field "
                            + fieldName
                            + " contained an unknown YesOrNo value for "
                            + datasetName()
                            + ". Record: "
                            + node,
                    ex);
        }
    }

    protected final LocalDate requiredLocalDate(JsonNode node, String fieldName) {
        val value = nullableLocalDate(node, fieldName);
        if (value == null) {
            throw invalidField(node, fieldName);
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
                    "CSDS field "
                            + fieldName
                            + " contained an invalid date for "
                            + datasetName()
                            + ". Record: "
                            + node,
                    ex);
        }
    }

    protected final AppRegistryException invalidField(String fieldName) {
        return new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "CSDS field " + fieldName + " was missing or invalid for " + datasetName());
    }

    protected final AppRegistryException invalidField(JsonNode node, String fieldName) {
        return new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "CSDS field "
                        + fieldName
                        + " was missing or invalid for "
                        + datasetName()
                        + ". Record: "
                        + node);
    }

    protected final void validateExpectedFields(JsonNode node, List<String> expectedFields) {
        val absentFields =
                expectedFields.stream().filter(fieldName -> !node.has(fieldName)).toList();
        if (!absentFields.isEmpty()) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS record for "
                            + datasetName()
                            + " was missing expected fields: "
                            + String.join(", ", absentFields));
        }
    }

    @Override
    public final void apply(D processedData) {
        applyWithAuditing(processedData);
    }

    protected final R applyWithAuditing(D processedData) {
        return csdsIngressTransactionRunner.execute(
                () -> {
                    val diff = diff(processedData);
                    logDiffSummary(diff);
                    report(processedData, diff);
                    var level = csdsAuditService.auditLevel();
                    try {
                        applyDiff(diff);
                        csdsAuditService.persistSuccessAudits(
                                level, buildSuccessAudits(processedData, diff));
                        return diff;
                    } catch (CsdsBatchUpsertException ex) {
                        csdsAuditService.persistFailureAudits(
                                level, buildFailureAudits(processedData, diff, ex));
                        throw ex;
                    }
                });
    }

    protected abstract R diff(D processedData);

    protected abstract List<CsdsAuditEntry> buildSuccessAudits(D processedData, R diff);

    protected abstract List<CsdsAuditEntry> buildFailureAudits(
            D processedData, R diff, CsdsBatchUpsertException ex);

    protected void logDiffSummary(R diff) {
        // Diff summary logging is optional per processor.
    }

    protected void report(D processedData, R diff) {
        // Reporting is optional per processor and can be implemented when configured.
    }

    protected void applyDiff(R diff) {
        // Database apply is optional until concrete update flows are introduced.
    }

    private String appendPagingParameters(String path, String pagingParameters) {
        return path + (path.contains("?") ? "&" : "?") + pagingParameters;
    }

    private JsonNode loadMockResponse(String mockFilePath) {
        try {
            val isClassPathUri = mockFilePath.startsWith("classpath:");
            val resourcePath =
                    isClassPathUri ? mockFilePath.substring("classpath:".length()) : mockFilePath;
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

            if (isClassPathUri) {
                log.warn(
                        "Configured CSDS mock response for {} was not found at {}. Falling back to endpoint.",
                        datasetName(),
                        mockFilePath);
                return null;
            }

            val filePath = Path.of(mockFilePath);
            if (Files.exists(filePath)) {
                val response = OBJECT_MAPPER.readTree(Files.readString(filePath));
                log.info("Loaded CSDS mock response for {} from {}", datasetName(), mockFilePath);
                return response;
            }

            log.warn(
                    "Configured CSDS mock response for {} was not found at {}. Falling back to endpoint.",
                    datasetName(),
                    mockFilePath);
            return null;

        } catch (IOException ex) {
            log.error(
                    "Failed to load CSDS mock response for {} from {}. Falling back to endpoint.",
                    datasetName(),
                    mockFilePath,
                    ex);
            return null;
        }
    }

    protected final Map<Long, JsonNode> indexSourceRecords(
            List<JsonNode> processedData, Function<JsonNode, Long> idExtractor) {
        return processedData.stream()
                .flatMap(page -> extractRecords(page).stream())
                .filter(node -> idExtractor.apply(node) != null)
                .collect(
                        java.util.stream.Collectors.toMap(
                                idExtractor,
                                Function.identity(),
                                (first, second) -> second,
                                java.util.LinkedHashMap::new));
    }

    protected final <T> List<CsdsAuditEntry> buildSuccessAuditEntries(
            List<IngressDiffRecord<T, T, T>> diffRecords,
            Map<Long, JsonNode> sourceById,
            ToLongFunction<T> idExtractor) {
        return diffRecords.stream()
                .map(item -> toSuccessAuditEntry(item, sourceById, idExtractor))
                .toList();
    }

    protected final <T> List<CsdsAuditEntry> buildFailureAuditEntries(
            List<IngressDiffRecord<T, T, T>> diffRecords,
            Map<Long, JsonNode> sourceById,
            ToLongFunction<T> idExtractor,
            Class<T> recordType,
            CsdsBatchUpsertException ex) {
        var actionsById =
                diffRecords.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        item -> idExtractor.applyAsLong(item.intended()),
                                        item -> item.operation().name(),
                                        (first, second) -> first,
                                        java.util.LinkedHashMap::new));
        return ex.failures().stream()
                .filter(item -> recordType.isInstance(item.item()))
                .map(
                        item ->
                                toFailureAuditEntry(
                                        item, sourceById, idExtractor, recordType, actionsById))
                .toList();
    }

    private <T> CsdsAuditEntry toFailureAuditEntry(
            FailedUpsertRecord<?> failure,
            Map<Long, JsonNode> sourceById,
            ToLongFunction<T> idExtractor,
            Class<T> recordType,
            Map<Long, String> actionsById) {
        var typedRecord = recordType.cast(failure.item());
        var key = idExtractor.applyAsLong(typedRecord);
        return createAuditEntry(
                actionsById.get(key), key, sourceById.get(key), failure.errorMessage());
    }

    private <T> CsdsAuditEntry toSuccessAuditEntry(
            IngressDiffRecord<T, T, T> item,
            Map<Long, JsonNode> sourceById,
            ToLongFunction<T> idExtractor) {
        var key = idExtractor.applyAsLong(item.intended());
        return createAuditEntry(item.operation().name(), key, sourceById.get(key), null);
    }

    private CsdsAuditEntry createAuditEntry(
            String action, Long key, JsonNode sourceRecord, String error) {
        return new CsdsAuditEntry(
                processorName().toUpperCase(Locale.ROOT),
                action,
                key,
                sourceRecord == null ? null : sourceRecord.toString(),
                error);
    }
}
