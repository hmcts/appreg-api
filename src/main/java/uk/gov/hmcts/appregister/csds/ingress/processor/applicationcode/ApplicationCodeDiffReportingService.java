package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

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
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;

@Slf4j
@Component
public class ApplicationCodeDiffReportingService {
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHANGE_TYPE_IGNORE = "ignore";
    private static final String CHANGE_TYPE_INSERT = "insert";
    private static final String CHANGE_TYPE_UPDATE = "update";

    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final String reportingDir;

    public ApplicationCodeDiffReportingService(
            CsdsIngressProperties properties,
            ApplicationListEntryRepository applicationListEntryRepository) {
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.reportingDir = properties.getProcessors().getApplicationCodes().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            ApplicationCodeDiffResult diffResult,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        if (!StringUtils.hasText(reportingDir)) {
            log.info("CSDS reporting disabled for {}", datasetName);
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
                Comparator.comparing(DiffReportRow::acId).thenComparing(DiffReportRow::changeType));

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
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val incomingRecordsByAcId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, "AC_ID") != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, "AC_ID"),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .map(
                        item ->
                                new DiffReportRow(
                                        nullableLong(
                                                incomingRecordsByAcId.get(item.intended().id()),
                                                "PSSApplicationCodeID"),
                                        nullableLong(
                                                incomingRecordsByAcId.get(item.intended().id()),
                                                "ApplicationCodeID"),
                                        item.intended().id(),
                                        changeType(item.operation())))
                .toList();
    }

    private int countByOperation(
            List<
                            IngressDiffRecord<
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord>>
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

    private Map<Long, Long> loadProtectedDeletionCounts(List<Long> applicationCodeIds) {
        if (applicationCodeIds.isEmpty()) {
            return Map.of();
        }

        return applicationListEntryRepository.countByApplicationCodeIds(applicationCodeIds).stream()
                .collect(
                        Collectors.toMap(
                                ApplicationListEntryRepository.ApplicationCodeReferenceCount
                                        ::getApplicationCodeId,
                                ApplicationListEntryRepository.ApplicationCodeReferenceCount
                                        ::getReferenceCount));
    }

    private void writeComparisonCsvFiles(
            String datasetName,
            List<JsonNode> processedData,
            Map<Long, ApplicationCodeIngressRecord> existingById,
            Map<Long, ApplicationCodeIngressRecord> incomingById,
            List<DiffReportRow> diffReport,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        try {
            val outputDir = Files.createDirectories(Path.of(reportingDir));
            val timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
            val incomingJsonPath =
                    outputDir.resolve("application_codes_incoming_" + timestamp + ".json");
            val incomingCsvPath =
                    outputDir.resolve("application_codes_incoming_" + timestamp + ".csv");
            val existingPath =
                    outputDir.resolve("application_codes_existing_" + timestamp + ".csv");
            val diffReportPath = outputDir.resolve("application_codes_diff_" + timestamp + ".csv");
            val protectedDeletionCounts =
                    loadProtectedDeletionCounts(new ArrayList<>(existingById.keySet()));

            Files.writeString(
                    incomingJsonPath,
                    buildIncomingJson(processedData, recordsExtractor),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    incomingCsvPath,
                    buildIncomingCsv(processedData, recordsExtractor),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    existingPath,
                    buildExistingCsv(existingById, protectedDeletionCounts),
                    StandardCharsets.UTF_8);
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

    private String buildExistingCsv(
            Map<Long, ApplicationCodeIngressRecord> existingById,
            Map<Long, Long> protectedDeletionCounts) {
        val csv = new StringBuilder(csvHeader(true));
        existingById.values().stream()
                .sorted(Comparator.comparing(ApplicationCodeIngressRecord::id))
                .map(item -> item.toCsvRow(protectedDeletionCounts.get(item.id())))
                .forEach(csv::append);
        return csv.toString();
    }

    private String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val csv = new StringBuilder(csvHeader(false));
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .map(this::toIncomingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    private String csvHeader(boolean includeReferenceCount) {
        val baseHeader =
                "pssApplicationCodeId,applicationCodeId,acId,code,title,wording,legislation,"
                        + "feeDue,requiresRespondent,startDate,endDate,"
                        + "bulkRespondentAllowed,version,feeReference";
        return includeReferenceCount ? baseHeader + ",referenceCount\n" : baseHeader + "\n";
    }

    private String buildDiffReportCsv(List<DiffReportRow> diffReport) {
        val csv = new StringBuilder("pssApplicationCodeId,applicationCodeId,acId,changeType\n");
        diffReport.stream().map(DiffReportRow::toCsvRow).forEach(csv::append);
        return csv.toString();
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSApplicationCodeID")),
                        csvValue(nullableLong(node, "ApplicationCodeID")),
                        csvValue(nullableLong(node, "AC_ID")),
                        csvValue(nullableText(node, "Code")),
                        csvValue(nullableText(node, "ApplicationTitle")),
                        csvValue(nullableText(node, "ApplicationWording")),
                        csvValue(nullableText(node, "Legislation")),
                        csvValue(nullableText(node, "FeeDue")),
                        csvValue(nullableText(node, "Respondent")),
                        csvValue(nullableText(node, "StartDate")),
                        csvValue(nullableText(node, "EndDate")),
                        csvValue(nullableText(node, "BulkRespondentAllowed")),
                        csvValue(nullableLong(node, "RevisionNumber")),
                        csvValue(nullableText(node, "FeeReference")))
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
            Long pssApplicationCodeId, Long applicationCodeId, Long acId, String changeType) {
        private String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(pssApplicationCodeId),
                            csvValue(applicationCodeId),
                            csvValue(acId),
                            csvValue(changeType))
                    + "\n";
        }
    }
}
