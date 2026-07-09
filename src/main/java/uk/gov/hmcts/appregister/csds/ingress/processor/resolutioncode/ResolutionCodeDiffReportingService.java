package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
public class ResolutionCodeDiffReportingService {
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHANGE_TYPE_IGNORE = "ignore";
    private static final String CHANGE_TYPE_INSERT = "insert";
    private static final String CHANGE_TYPE_UPDATE = "update";

    private final String reportingDir;

    public ResolutionCodeDiffReportingService(CsdsIngressProperties properties) {
        this.reportingDir = properties.getProcessors().getResolutionCodes().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            ResolutionCodeDiffResult diffResult,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        if (!StringUtils.hasText(reportingDir)) {
            log.debug("CSDS reporting disabled for {}", datasetName);
            return;
        }

        val incomingById = diffResult.incomingById();
        val existingById = diffResult.existingById();
        val diffReport =
                new ArrayList<>(
                        buildDiffReport(processedData, diffResult.diffRecords(), recordsExtractor));
        val insertedCount = countByOperation(diffResult.diffRecords(), IngressOperation.INSERT);
        val updatedCount = countByOperation(diffResult.diffRecords(), IngressOperation.UPDATE);
        val unchangedCount = countByOperation(diffResult.diffRecords(), IngressOperation.IGNORE);

        diffReport.sort(
                Comparator.comparing(DiffReportRow::rcId).thenComparing(DiffReportRow::changeType));

        writeComparisonCsvFiles(
                datasetName,
                processedData,
                existingById,
                incomingById,
                diffReport,
                recordsExtractor);

        log.info(
                "CSDS diff for {} on {}.{}: incoming={}, existing={}, inserts={}, updates={}, ignores={}",
                datasetName,
                targetTable,
                targetKeyField,
                incomingById.size(),
                existingById.size(),
                insertedCount,
                updatedCount,
                unchangedCount);
    }

    private List<DiffReportRow> buildDiffReport(
            List<JsonNode> processedData,
            List<
                            IngressDiffRecord<
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val incomingRecordsByRcId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, "RC_ID") != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, "RC_ID"),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .map(
                        item ->
                                new DiffReportRow(
                                        nullableLong(
                                                incomingRecordsByRcId.get(item.intended().id()),
                                                "PSSRCID"),
                                        nullableLong(
                                                incomingRecordsByRcId.get(item.intended().id()),
                                                "ResolutionCodeID"),
                                        item.intended().id(),
                                        changeType(item.operation())))
                .toList();
    }

    private int countByOperation(
            List<
                            IngressDiffRecord<
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord>>
                    diffRecords,
            IngressOperation operation) {
        return Math.toIntExact(
                diffRecords.stream().filter(item -> item.operation() == operation).count());
    }

    private String changeType(IngressOperation operation) {
        return switch (operation) {
            case INSERT -> CHANGE_TYPE_INSERT;
            case UPDATE -> CHANGE_TYPE_UPDATE;
            case IGNORE -> CHANGE_TYPE_IGNORE;
        };
    }

    private void writeComparisonCsvFiles(
            String datasetName,
            List<JsonNode> processedData,
            Map<Long, ResolutionCodeIngressRecord> existingById,
            Map<Long, ResolutionCodeIngressRecord> incomingById,
            List<DiffReportRow> diffReport,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        try {
            val outputDir = Files.createDirectories(Path.of(reportingDir));
            val timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
            val incomingJsonPath =
                    outputDir.resolve("resolution_codes_incoming_" + timestamp + ".json");
            val incomingCsvPath =
                    outputDir.resolve("resolution_codes_incoming_" + timestamp + ".csv");
            val existingPath = outputDir.resolve("resolution_codes_existing_" + timestamp + ".csv");
            val diffReportPath = outputDir.resolve("resolution_codes_diff_" + timestamp + ".csv");

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
            val emptyPayload = OBJECT_MAPPER.createObjectNode();
            emptyPayload.putArray("records");
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(emptyPayload);
        }

        val mergedPayload = cloneWithoutRecords(processedData.getFirst());
        val mergedRecords = mergedPayload.putArray("records");
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .forEach(mergedRecords::add);
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(mergedPayload);
    }

    private ObjectNode cloneWithoutRecords(JsonNode page) {
        val clonedPage =
                page instanceof ObjectNode objectNode
                        ? objectNode.deepCopy()
                        : OBJECT_MAPPER.createObjectNode();
        clonedPage.remove("records");
        return clonedPage;
    }

    private String buildExistingCsv(Map<Long, ResolutionCodeIngressRecord> existingById) {
        val csv = new StringBuilder(csvHeader());
        existingById.values().stream()
                .sorted(Comparator.comparing(ResolutionCodeIngressRecord::id))
                .map(this::toExistingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    private String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val csv = new StringBuilder(csvHeader());
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .map(this::toIncomingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    private String csvHeader() {
        return "pssResolutionCodeId,resolutionCodeId,rcId,code,title,wording,legislation,"
                + "recipient1Email,recipient2Email,startDate,endDate,version\n";
    }

    private String buildDiffReportCsv(List<DiffReportRow> diffReport) {
        val csv = new StringBuilder("pssResolutionCodeId,resolutionCodeId,rcId,changeType\n");
        diffReport.stream().map(DiffReportRow::toCsvRow).forEach(csv::append);
        return csv.toString();
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSRCID")),
                        csvValue(nullableLong(node, "ResolutionCodeID")),
                        csvValue(nullableLong(node, "RC_ID")),
                        csvValue(nullableText(node, "Code")),
                        csvValue(nullableText(node, "ResultTitle")),
                        csvValue(nullableText(node, "ResultWording")),
                        csvValue(nullableText(node, "Legislation")),
                        csvValue(nullableText(node, "Recipient1Email")),
                        csvValue(nullableText(node, "Recipient2Email")),
                        csvValue(nullableText(node, "StartDate")),
                        csvValue(nullableText(node, "EndDate")),
                        csvValue(nullableLong(node, "RevisionNumber")))
                + "\n";
    }

    private String toExistingCsvRow(ResolutionCodeIngressRecord item) {
        return String.join(
                        ",",
                        csvValue((Object) null),
                        csvValue((Object) null),
                        csvValue(item.id()),
                        csvValue(item.code()),
                        csvValue(item.title()),
                        csvValue(item.wording()),
                        csvValue(item.legislation()),
                        csvValue(item.recipient1Email()),
                        csvValue(item.recipient2Email()),
                        csvValue(item.startDate()),
                        csvValue(item.endDate()),
                        csvValue(item.version()))
                + "\n";
    }

    private Long nullableLong(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        val field = node.get(fieldName);
        if (field == null || !field.canConvertToLong()) {
            return null;
        }

        return field.longValue();
    }

    private String nullableText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        val field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    static String csvValue(Object value) {
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

    private record DiffReportRow(
            Long pssResolutionCodeId, Long resolutionCodeId, Long rcId, String changeType) {
        private String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(pssResolutionCodeId),
                            csvValue(resolutionCodeId),
                            csvValue(rcId),
                            csvValue(changeType))
                    + "\n";
        }
    }
}
