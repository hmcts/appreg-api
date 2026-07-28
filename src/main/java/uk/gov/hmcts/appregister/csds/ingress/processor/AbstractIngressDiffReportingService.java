package uk.gov.hmcts.appregister.csds.ingress.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

public abstract class AbstractIngressDiffReportingService<R> {
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Clock REPORTING_CLOCK = Clock.systemUTC();
    private static final String CHANGE_TYPE_INSERT = "insert";
    private static final String CHANGE_TYPE_UPDATE = "update";
    private static final String RECORDS_FIELD = "records";

    protected final void reportDiff(
            String reportingDir,
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            Map<Long, R> incomingById,
            Map<Long, R> existingById,
            List<IngressDiffRecord<R, R, R>> diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var log = LoggerFactory.getLogger(getClass());
        if (!StringUtils.hasText(reportingDir)) {
            log.debug("CSDS reporting disabled for {}", datasetName);
            return;
        }

        var diffReport =
                new ArrayList<>(buildDiffReport(processedData, diffRecords, recordsExtractor));
        diffReport.sort(
                Comparator.comparing(
                                DiffReportCsvRow::sortId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(DiffReportCsvRow::changeType));

        writeComparisonCsvFiles(
                reportingDir,
                datasetName,
                processedData,
                existingById,
                diffReport,
                recordsExtractor);

        log.info(
                "CSDS diff for {} on {}.{}: incoming={}, existing={}, inserts={}, updates={}",
                datasetName,
                targetTable,
                targetKeyField,
                incomingById.size(),
                existingById.size(),
                countByOperation(diffRecords, IngressOperation.INSERT),
                countByOperation(diffRecords, IngressOperation.UPDATE));
    }

    protected abstract String filePrefix();

    protected abstract List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<IngressDiffRecord<R, R, R>> diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor);

    protected abstract String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor);

    protected abstract String buildExistingCsv(Map<Long, R> existingById);

    protected abstract String diffReportHeader();

    protected final String changeType(IngressOperation operation) {
        return switch (operation) {
            case INSERT -> CHANGE_TYPE_INSERT;
            case UPDATE -> CHANGE_TYPE_UPDATE;
        };
    }

    protected final String buildDiffReportCsv(List<DiffReportCsvRow> diffReport) {
        var csv = new StringBuilder(diffReportHeader());
        diffReport.stream().map(DiffReportCsvRow::toCsvRow).forEach(csv::append);
        return csv.toString();
    }

    protected final String buildCsv(String header, Stream<String> rows) {
        var csv = new StringBuilder(header);
        rows.forEach(csv::append);
        return csv.toString();
    }

    protected static Long nullableLong(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        var field = node.get(fieldName);
        if (field == null || !field.canConvertToLong()) {
            return null;
        }

        return field.longValue();
    }

    protected static String nullableText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        var field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    protected static String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        return "\""
                + value.toString()
                        .replace("\r\n", "\\n")
                        .replace("\n", "\\n")
                        .replace("\r", "\\n")
                        .replace("\"", "\"\"")
                + "\"";
    }

    private int countByOperation(
            List<IngressDiffRecord<R, R, R>> diffRecords, IngressOperation operation) {
        return Math.toIntExact(
                diffRecords.stream().filter(item -> item.operation() == operation).count());
    }

    private void writeComparisonCsvFiles(
            String reportingDir,
            String datasetName,
            List<JsonNode> processedData,
            Map<Long, R> existingById,
            List<DiffReportCsvRow> diffReport,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var log = LoggerFactory.getLogger(getClass());
        try {
            var outputDir = Files.createDirectories(Path.of(reportingDir));
            var timestamp = LocalDateTime.now(REPORTING_CLOCK).format(FILE_TIMESTAMP_FORMAT);
            var incomingJsonPath =
                    outputDir.resolve(filePrefix() + "_incoming_" + timestamp + ".json");
            var incomingCsvPath =
                    outputDir.resolve(filePrefix() + "_incoming_" + timestamp + ".csv");
            var existingPath = outputDir.resolve(filePrefix() + "_existing_" + timestamp + ".csv");
            var diffReportPath = outputDir.resolve(filePrefix() + "_diff_" + timestamp + ".csv");

            Files.writeString(
                    incomingJsonPath,
                    buildIncomingJson(processedData, recordsExtractor),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    incomingCsvPath,
                    buildIncomingCsv(processedData, recordsExtractor),
                    StandardCharsets.UTF_8);
            Files.writeString(existingPath, buildExistingCsv(existingById), StandardCharsets.UTF_8);
            Files.writeString(
                    diffReportPath, buildDiffReportCsv(diffReport), StandardCharsets.UTF_8);

            log.info(
                    "Wrote CSDS comparison artifacts for {} to {}, {}, {} and {}",
                    datasetName,
                    incomingJsonPath,
                    incomingCsvPath,
                    existingPath,
                    diffReportPath);
        } catch (IOException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Failed to write CSDS comparison CSV files for " + datasetName,
                    ex);
        }
    }

    private String buildIncomingJson(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor)
            throws IOException {
        if (processedData.isEmpty()) {
            var emptyPayload = OBJECT_MAPPER.createObjectNode();
            emptyPayload.putArray(RECORDS_FIELD);
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(emptyPayload);
        }

        var mergedPayload = cloneWithoutRecords(processedData.getFirst());
        var mergedRecords = mergedPayload.putArray(RECORDS_FIELD);
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .forEach(mergedRecords::add);
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(mergedPayload);
    }

    private ObjectNode cloneWithoutRecords(JsonNode page) {
        var clonedPage =
                page instanceof ObjectNode objectNode
                        ? objectNode.deepCopy()
                        : OBJECT_MAPPER.createObjectNode();
        clonedPage.remove(RECORDS_FIELD);
        return clonedPage;
    }

    protected interface DiffReportCsvRow {
        Long sortId();

        String changeType();

        String toCsvRow();
    }
}
