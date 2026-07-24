package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

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
public class ApplicationCodeDiffReportingService
        extends AbstractIngressDiffReportingService<ApplicationCodeIngressRecord> {
    private static final String AC_ID = "AC_ID";
    private final String reportingDir;

    public ApplicationCodeDiffReportingService(CsdsIngressProperties properties) {
        this.reportingDir = properties.getProcessors().getApplicationCodes().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            ApplicationCodeDiffResult diffResult,
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
        return "application_codes";
    }

    @Override
    protected List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<
                            IngressDiffRecord<
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord,
                                    ApplicationCodeIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var incomingRecordsByAcId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, AC_ID) != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, AC_ID),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
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

    @Override
    protected String buildExistingCsv(Map<Long, ApplicationCodeIngressRecord> existingById) {
        var csv = new StringBuilder(csvHeader());
        existingById.values().stream()
                .sorted(Comparator.comparing(ApplicationCodeIngressRecord::id))
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
        return "pssApplicationCodeId,applicationCodeId,acId,code,title,wording,legislation,"
                + "feeDue,requiresRespondent,startDate,endDate,"
                + "bulkRespondentAllowed,version,feeReference\n";
    }

    @Override
    protected String diffReportHeader() {
        return "pssApplicationCodeId,applicationCodeId,acId,changeType\n";
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSApplicationCodeID")),
                        csvValue(nullableLong(node, "ApplicationCodeID")),
                        csvValue(nullableLong(node, AC_ID)),
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

    private String toExistingCsvRow(ApplicationCodeIngressRecord item) {
        return String.join(
                        ",",
                        csvValue((Object) null),
                        csvValue((Object) null),
                        csvValue(item.id()),
                        csvValue(item.code()),
                        csvValue(item.title()),
                        csvValue(item.wording()),
                        csvValue(item.legislation()),
                        csvValue(item.feeDue()),
                        csvValue(item.requiresRespondent()),
                        csvValue(item.startDate()),
                        csvValue(item.endDate()),
                        csvValue(item.bulkRespondentAllowed()),
                        csvValue(item.version()),
                        csvValue(item.feeReference()))
                + "\n";
    }

    private record DiffReportRow(
            Long pssApplicationCodeId, Long applicationCodeId, Long acId, String changeType)
            implements DiffReportCsvRow {
        @Override
        public Long sortId() {
            return acId;
        }

        @Override
        public String toCsvRow() {
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
