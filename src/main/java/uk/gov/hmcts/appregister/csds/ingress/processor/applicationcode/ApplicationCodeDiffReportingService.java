package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final int CHANGE_LOG_LIMIT = 20;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
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
        val diffReport = new ArrayList<>(buildDiffReport(diffResult.diffRecords()));
        val insertedCount = countByOperation(diffResult.diffRecords(), IngressOperation.INSERT);
        val updatedCount = countByOperation(diffResult.diffRecords(), IngressOperation.UPDATE);
        val unchangedCount = countByOperation(diffResult.diffRecords(), IngressOperation.IGNORE);

        diffReport.sort(
                Comparator.comparing(DiffReportRow::id).thenComparing(DiffReportRow::changeType));

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

        logChangedIds(
                CHANGE_TYPE_INSERT,
                datasetName,
                targetTable,
                targetKeyField,
                filterIdsByOperation(diffResult.diffRecords(), IngressOperation.INSERT),
                String::valueOf);
        logChangedIds(
                CHANGE_TYPE_UPDATE,
                datasetName,
                targetTable,
                targetKeyField,
                filterIdsByOperation(diffResult.diffRecords(), IngressOperation.UPDATE),
                String::valueOf);
        logChangedIds(
                CHANGE_TYPE_IGNORE,
                datasetName,
                targetTable,
                targetKeyField,
                filterIdsByOperation(diffResult.diffRecords(), IngressOperation.IGNORE),
                String::valueOf);
    }

    private <T> void logChangedIds(
            String changeType,
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<T> values,
            Function<T, String> formatter) {
        if (values.isEmpty()) {
            return;
        }

        val preview =
                values.stream()
                        .limit(CHANGE_LOG_LIMIT)
                        .map(formatter)
                        .collect(Collectors.joining(", "));
        val truncated = values.size() > CHANGE_LOG_LIMIT ? " (truncated)" : "";

        log.info(
                "CSDS {} preview for {} on {}.{}: {}{}",
                changeType,
                datasetName,
                targetTable,
                targetKeyField,
                preview,
                truncated);
    }

    private List<DiffReportRow> buildDiffReport(
            List<IngressDiffRecord<ApplicationCodeIngressRecord, ApplicationCodeIngressRecord>>
                    diffRecords) {
        return diffRecords.stream()
                .map(
                        record ->
                                new DiffReportRow(
                                        record.incoming().id(), changeType(record.operation())))
                .toList();
    }

    private int countByOperation(
            List<IngressDiffRecord<ApplicationCodeIngressRecord, ApplicationCodeIngressRecord>>
                    diffRecords,
            IngressOperation operation) {
        return Math.toIntExact(
                diffRecords.stream().filter(record -> record.operation() == operation).count());
    }

    private List<Long> filterIdsByOperation(
            List<IngressDiffRecord<ApplicationCodeIngressRecord, ApplicationCodeIngressRecord>>
                    diffRecords,
            IngressOperation operation) {
        return diffRecords.stream()
                .filter(record -> record.operation() == operation)
                .map(record -> record.incoming().id())
                .toList();
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
            val rawIncomingPath =
                    outputDir.resolve("application_codes_incoming_raw_" + timestamp + ".csv");
            val incomingPath =
                    outputDir.resolve("application_codes_incoming_" + timestamp + ".csv");
            val existingPath =
                    outputDir.resolve("application_codes_existing_" + timestamp + ".csv");
            val diffReportPath = outputDir.resolve("application_codes_diff_" + timestamp + ".csv");
            val protectedDeletionCounts =
                    loadProtectedDeletionCounts(new ArrayList<>(existingById.keySet()));

            Files.writeString(
                    rawIncomingPath,
                    buildRawIncomingCsv(processedData, recordsExtractor),
                    StandardCharsets.UTF_8);
            Files.writeString(incomingPath, buildIncomingCsv(incomingById), StandardCharsets.UTF_8);
            Files.writeString(
                    existingPath,
                    buildExistingCsv(existingById, protectedDeletionCounts),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    diffReportPath, buildDiffReportCsv(diffReport), StandardCharsets.UTF_8);

            log.info(
                    "Wrote CSDS comparison CSV files for {} to {}, {}, {} and {}",
                    datasetName,
                    rawIncomingPath,
                    incomingPath,
                    existingPath,
                    diffReportPath);
        } catch (IOException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "Failed to write CSDS comparison CSV files for " + datasetName,
                    ex);
        }
    }

    private String buildRawIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val pageMetadataFields = new LinkedHashSet<String>();
        val recordFields = new LinkedHashSet<String>();

        for (val page : processedData) {
            page.fieldNames()
                    .forEachRemaining(
                            fieldName -> {
                                if (!"records".equals(fieldName)) {
                                    pageMetadataFields.add(fieldName);
                                }
                            });
            recordsExtractor
                    .apply(page)
                    .forEach(record -> record.fieldNames().forEachRemaining(recordFields::add));
        }

        val csv = new StringBuilder("pageIndex,recordIndex");
        pageMetadataFields.forEach(fieldName -> csv.append(",page_").append(fieldName));
        recordFields.forEach(fieldName -> csv.append(",").append(fieldName));
        csv.append("\n");

        val rows = new ArrayList<RawIncomingCsvRow>();
        for (var pageIndex = 0; pageIndex < processedData.size(); pageIndex++) {
            val page = processedData.get(pageIndex);
            val records = recordsExtractor.apply(page);

            for (var recordIndex = 0; recordIndex < records.size(); recordIndex++) {
                val record = records.get(recordIndex);
                rows.add(
                        new RawIncomingCsvRow(
                                extractRecordId(record), pageIndex, recordIndex, page, record));
            }
        }

        rows.stream()
                .sorted(
                        Comparator.comparing(
                                        RawIncomingCsvRow::id,
                                        Comparator.nullsLast(Long::compareTo))
                                .thenComparing(RawIncomingCsvRow::pageIndex)
                                .thenComparing(RawIncomingCsvRow::recordIndex))
                .forEach(
                        row -> {
                            csv.append(csvValue(row.pageIndex()))
                                    .append(",")
                                    .append(csvValue(row.recordIndex()));
                            appendPageMetadata(csv, row.page(), pageMetadataFields);
                            appendRecordFields(csv, row.record(), recordFields);
                            csv.append("\n");
                        });

        return csv.toString();
    }

    private void appendPageMetadata(
            StringBuilder csv, JsonNode page, Set<String> pageMetadataFields) {
        pageMetadataFields.forEach(
                fieldName -> csv.append(",").append(csvValue(jsonValue(page.get(fieldName)))));
    }

    private void appendRecordFields(StringBuilder csv, JsonNode record, Set<String> recordFields) {
        recordFields.forEach(
                fieldName -> csv.append(",").append(csvValue(jsonValue(record.get(fieldName)))));
    }

    private String buildIncomingCsv(Map<Long, ApplicationCodeIngressRecord> incomingById) {
        val csv = new StringBuilder(csvHeader(false));
        incomingById.values().stream()
                .sorted(Comparator.comparing(ApplicationCodeIngressRecord::id))
                .map(ApplicationCodeIngressRecord::toCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    private String buildExistingCsv(
            Map<Long, ApplicationCodeIngressRecord> existingById,
            Map<Long, Long> protectedDeletionCounts) {
        val csv = new StringBuilder(csvHeader(true));
        existingById.values().stream()
                .sorted(Comparator.comparing(ApplicationCodeIngressRecord::id))
                .map(record -> record.toCsvRow(protectedDeletionCounts.get(record.id())))
                .forEach(csv::append);
        return csv.toString();
    }

    private String csvHeader(boolean includeReferenceCount) {
        val baseHeader =
                "id,code,title,wording,legislation,feeDue,requiresRespondent,startDate,endDate,"
                        + "bulkRespondentAllowed,version,feeReference";
        return includeReferenceCount ? baseHeader + ",referenceCount\n" : baseHeader + "\n";
    }

    private String buildDiffReportCsv(List<DiffReportRow> diffReport) {
        val csv = new StringBuilder("id,changeType\n");
        diffReport.stream().map(DiffReportRow::toCsvRow).forEach(csv::append);
        return csv.toString();
    }

    private Long extractRecordId(JsonNode record) {
        return ApplicationCodeIngressRecord.resolveId(record);
    }

    private static String jsonValue(JsonNode field) {
        if (field == null || field.isNull()) {
            return null;
        }

        return field.isValueNode() ? field.asText() : field.toString();
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

    private record DiffReportRow(Long id, String changeType) {
        private String toCsvRow() {
            return String.join(",", csvValue(id), csvValue(changeType)) + "\n";
        }
    }

    private record RawIncomingCsvRow(
            Long id, int pageIndex, int recordIndex, JsonNode page, JsonNode record) {}
}
