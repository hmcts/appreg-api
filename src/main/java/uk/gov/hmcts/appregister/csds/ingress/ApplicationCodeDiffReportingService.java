package uk.gov.hmcts.appregister.csds.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationCodeRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

@Slf4j
@Component
class ApplicationCodeDiffReportingService {
    private static final int CHANGE_LOG_LIMIT = 20;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    private final ApplicationCodeRepository applicationCodeRepository;
    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final String comparisonOutputDir;

    ApplicationCodeDiffReportingService(
            CsdsIngressProperties properties,
            ApplicationCodeRepository applicationCodeRepository,
            ApplicationListEntryRepository applicationListEntryRepository) {
        this.applicationCodeRepository = applicationCodeRepository;
        this.applicationListEntryRepository = applicationListEntryRepository;
        this.comparisonOutputDir =
                properties.getProcessors().getApplicationCodes().getComparisonOutputDir();
    }

    void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            Function<JsonNode, ApplicationCodeIngressRecord> recordMapper,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        val incomingById =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .map(recordMapper)
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        record -> record,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val existingById =
                applicationCodeRepository.findAll().stream()
                        .map(ApplicationCodeIngressRecord::fromEntity)
                        .collect(
                                Collectors.toMap(
                                        ApplicationCodeIngressRecord::id,
                                        record -> record,
                                        (first, second) -> second,
                                        LinkedHashMap::new));

        val insertedIds =
                incomingById.keySet().stream()
                        .filter(id -> !existingById.containsKey(id))
                        .sorted()
                        .toList();
        val deletedIds =
                existingById.keySet().stream()
                        .filter(id -> !incomingById.containsKey(id))
                        .sorted()
                        .toList();
        val protectedDeletionCounts = loadProtectedDeletionCounts(deletedIds);
        val updatedRecords = new ArrayList<ChangedApplicationCodeRecord>();
        var unchangedCount = 0;

        for (val entry : incomingById.entrySet()) {
            val existing = existingById.get(entry.getKey());
            if (existing == null) {
                continue;
            }

            val incoming = entry.getValue();
            if (existing.equals(incoming)) {
                unchangedCount++;
                continue;
            }

            updatedRecords.add(
                    new ChangedApplicationCodeRecord(
                            incoming.id(), determineChangedFields(existing, incoming)));
        }

        val diffReport =
                buildDiffReport(insertedIds, deletedIds, updatedRecords, protectedDeletionCounts);

        writeComparisonCsvFiles(
                datasetName,
                processedData,
                existingById,
                incomingById,
                protectedDeletionCounts,
                diffReport,
                recordsExtractor);

        log.info(
                "CSDS diff for {} on {}.{}: incoming={}, existing={}, inserts={}, updates={}, deletes={}, unchanged={}",
                datasetName,
                targetTable,
                targetKeyField,
                incomingById.size(),
                existingById.size(),
                insertedIds.size(),
                updatedRecords.size(),
                deletedIds.size(),
                unchangedCount);

        logChangedIds(
                "insert", datasetName, targetTable, targetKeyField, insertedIds, String::valueOf);
        logChangedIds(
                "delete", datasetName, targetTable, targetKeyField, deletedIds, String::valueOf);
        logProtectedDeletions(datasetName, targetTable, targetKeyField, protectedDeletionCounts);
        logChangedIds(
                "update",
                datasetName,
                targetTable,
                targetKeyField,
                updatedRecords,
                changedRecord ->
                        changedRecord.id() + " changed fields " + changedRecord.changedFields());
    }

    private List<String> determineChangedFields(
            ApplicationCodeIngressRecord existing, ApplicationCodeIngressRecord incoming) {
        val changedFields = new ArrayList<String>();

        addChangedField(changedFields, "code", existing.code(), incoming.code());
        addChangedField(changedFields, "title", existing.title(), incoming.title());
        addChangedField(changedFields, "wording", existing.wording(), incoming.wording());
        addChangedField(
                changedFields, "legislation", existing.legislation(), incoming.legislation());
        addChangedField(changedFields, "feeDue", existing.feeDue(), incoming.feeDue());
        addChangedField(
                changedFields,
                "requiresRespondent",
                existing.requiresRespondent(),
                incoming.requiresRespondent());
        addChangedField(changedFields, "startDate", existing.startDate(), incoming.startDate());
        addChangedField(changedFields, "endDate", existing.endDate(), incoming.endDate());
        addChangedField(
                changedFields,
                "bulkRespondentAllowed",
                existing.bulkRespondentAllowed(),
                incoming.bulkRespondentAllowed());
        addChangedField(changedFields, "version", existing.version(), incoming.version());
        addChangedField(
                changedFields, "feeReference", existing.feeReference(), incoming.feeReference());

        return List.copyOf(changedFields);
    }

    private void addChangedField(
            List<String> changedFields,
            String fieldName,
            Object existingValue,
            Object incomingValue) {
        if (!Objects.equals(existingValue, incomingValue)) {
            changedFields.add(fieldName);
        }
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

    private Map<Long, Long> loadProtectedDeletionCounts(List<Long> deletedIds) {
        if (deletedIds.isEmpty()) {
            return Map.of();
        }

        return applicationListEntryRepository.countByApplicationCodeIds(deletedIds).stream()
                .collect(
                        Collectors.toMap(
                                ApplicationListEntryRepository.ApplicationCodeReferenceCount
                                        ::getApplicationCodeId,
                                ApplicationListEntryRepository.ApplicationCodeReferenceCount
                                        ::getReferenceCount));
    }

    private void logProtectedDeletions(
            String datasetName,
            String targetTable,
            String targetKeyField,
            Map<Long, Long> protectedDeletionCounts) {
        if (protectedDeletionCounts.isEmpty()) {
            return;
        }

        val preview =
                protectedDeletionCounts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .limit(CHANGE_LOG_LIMIT)
                        .map(entry -> entry.getKey() + " references=" + entry.getValue())
                        .collect(Collectors.joining(", "));
        val truncated = protectedDeletionCounts.size() > CHANGE_LOG_LIMIT ? " (truncated)" : "";

        log.info(
                "CSDS protected delete preview for {} on {}.{}: {}{}",
                datasetName,
                targetTable,
                targetKeyField,
                preview,
                truncated);
    }

    private void writeComparisonCsvFiles(
            String datasetName,
            List<JsonNode> processedData,
            Map<Long, ApplicationCodeIngressRecord> existingById,
            Map<Long, ApplicationCodeIngressRecord> incomingById,
            Map<Long, Long> protectedDeletionCounts,
            List<DiffReportRow> diffReport,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        if (!StringUtils.hasText(comparisonOutputDir)) {
            return;
        }

        try {
            val outputDir = Files.createDirectories(Path.of(comparisonOutputDir));
            val timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
            val rawIncomingPath =
                    outputDir.resolve("application_codes_incoming_raw_" + timestamp + ".csv");
            val incomingPath =
                    outputDir.resolve("application_codes_incoming_" + timestamp + ".csv");
            val existingPath =
                    outputDir.resolve("application_codes_existing_" + timestamp + ".csv");
            val diffReportPath = outputDir.resolve("application_codes_diff_" + timestamp + ".csv");

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
        csv.append(",rawRecordJson\n");

        for (var pageIndex = 0; pageIndex < processedData.size(); pageIndex++) {
            val page = processedData.get(pageIndex);
            val records = recordsExtractor.apply(page);

            for (var recordIndex = 0; recordIndex < records.size(); recordIndex++) {
                val record = records.get(recordIndex);
                csv.append(csvValue(pageIndex)).append(",").append(csvValue(recordIndex));
                appendPageMetadata(csv, page, pageMetadataFields);
                appendRecordFields(csv, record, recordFields);
                csv.append(",").append(csvValue(record.toString())).append("\n");
            }
        }

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

    private List<DiffReportRow> buildDiffReport(
            List<Long> insertedIds,
            List<Long> deletedIds,
            List<ChangedApplicationCodeRecord> updatedRecords,
            Map<Long, Long> protectedDeletionCounts) {
        val diffReport = new ArrayList<DiffReportRow>();
        insertedIds.forEach(id -> diffReport.add(new DiffReportRow(id, "insert", List.of(), null)));
        deletedIds.forEach(
                id ->
                        diffReport.add(
                                new DiffReportRow(
                                        id,
                                        "delete",
                                        List.of(),
                                        protectedDeletionCounts.containsKey(id))));
        updatedRecords.forEach(
                record ->
                        diffReport.add(
                                new DiffReportRow(
                                        record.id(), "update", record.changedFields(), null)));
        diffReport.sort(
                Comparator.comparing(DiffReportRow::id).thenComparing(DiffReportRow::changeType));
        return List.copyOf(diffReport);
    }

    private String buildDiffReportCsv(List<DiffReportRow> diffReport) {
        val csv = new StringBuilder("id,changeType,changedFields,referencedByRi\n");
        diffReport.stream().map(DiffReportRow::toCsvRow).forEach(csv::append);
        return csv.toString();
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

    private record ChangedApplicationCodeRecord(Long id, List<String> changedFields) {}

    private record DiffReportRow(
            Long id, String changeType, List<String> changedFields, Boolean referencedByRi) {
        private String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(id),
                            csvValue(changeType),
                            csvValue(String.join(", ", changedFields)),
                            csvValue(referencedByRi))
                    + "\n";
        }
    }
}
