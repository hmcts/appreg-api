package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractIngressDiffReportingService;

@Component
public class StandardApplicantDiffReportingService
        extends AbstractIngressDiffReportingService<StandardApplicantIngressRecord> {
    private final String reportingDir;

    public StandardApplicantDiffReportingService(CsdsIngressProperties properties) {
        reportingDir = properties.getProcessors().getStandardApplicants().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            StandardApplicantDiffResult diffResult,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        super.reportDiff(
                reportingDir,
                datasetName,
                targetTable,
                targetKeyField,
                processedData,
                diffResult.incomingById(),
                diffResult.existingById(),
                diffResult.diffRecords(),
                recordsExtractor);
    }

    @Override
    protected String filePrefix() {
        return "standard_applicants";
    }

    @Override
    protected List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<
                            IngressDiffRecord<
                                    StandardApplicantIngressRecord,
                                    StandardApplicantIngressRecord,
                                    StandardApplicantIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var incomingRecordsBySaId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, "SA_ID") != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, "SA_ID"),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
                        item -> {
                            var source = incomingRecordsBySaId.get(item.intended().id());
                            return new DiffReportRow(
                                    nullableLong(source, "PSSSAID"),
                                    nullableLong(source, "ApplicantID"),
                                    item.intended().id(),
                                    changeType(item.operation()));
                        })
                .toList();
    }

    @Override
    protected String buildExistingCsv(Map<Long, StandardApplicantIngressRecord> existingById) {
        var csv = new StringBuilder(csvHeader());
        existingById.values().stream()
                .sorted(Comparator.comparing(StandardApplicantIngressRecord::id))
                .map(this::toExistingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    @Override
    protected String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var csv = new StringBuilder(csvHeader());
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .map(this::toIncomingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    private String csvHeader() {
        return "psssaId,applicantId,saId,code,name,startDate,endDate,version\n";
    }

    @Override
    protected String diffReportHeader() {
        return "psssaId,applicantId,saId,changeType\n";
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSSAID")),
                        csvValue(nullableLong(node, "ApplicantID")),
                        csvValue(nullableLong(node, "SA_ID")),
                        csvValue(nullableText(node, "Code")),
                        csvValue(nullableText(node, "OrganisationName")),
                        csvValue(nullableText(node, "StartDate")),
                        csvValue(nullableText(node, "Enddate")),
                        csvValue(nullableLong(node, "RevisionNumber")))
                + "\n";
    }

    private String toExistingCsvRow(StandardApplicantIngressRecord item) {
        return String.join(
                        ",",
                        csvValue((Object) null),
                        csvValue((Object) null),
                        csvValue(item.id()),
                        csvValue(item.code()),
                        csvValue(item.name()),
                        csvValue(item.startDate()),
                        csvValue(item.endDate()),
                        csvValue(item.version()))
                + "\n";
    }

    private record DiffReportRow(Long psssaId, Long applicantId, Long saId, String changeType)
            implements DiffReportCsvRow {
        @Override
        public Long sortId() {
            return saId;
        }

        @Override
        public String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(psssaId),
                            csvValue(applicantId),
                            csvValue(saId),
                            csvValue(changeType))
                    + "\n";
        }
    }
}
