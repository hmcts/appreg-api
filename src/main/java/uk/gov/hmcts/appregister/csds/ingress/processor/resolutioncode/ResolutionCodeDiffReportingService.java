package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

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
public class ResolutionCodeDiffReportingService
        extends AbstractIngressDiffReportingService<ResolutionCodeIngressRecord> {
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
        return "resolution_codes";
    }

    @Override
    protected List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<
                            IngressDiffRecord<
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord,
                                    ResolutionCodeIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var incomingRecordsByRcId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, "RC_ID") != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, "RC_ID"),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
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

    @Override
    protected String buildExistingCsv(Map<Long, ResolutionCodeIngressRecord> existingById) {
        var csv = new StringBuilder(csvHeader());
        existingById.values().stream()
                .sorted(Comparator.comparing(ResolutionCodeIngressRecord::id))
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
        return "pssResolutionCodeId,resolutionCodeId,rcId,code,title,wording,legislation,"
                + "recipient1Email,recipient2Email,startDate,endDate,version\n";
    }

    @Override
    protected String diffReportHeader() {
        return "pssResolutionCodeId,resolutionCodeId,rcId,changeType\n";
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

    private record DiffReportRow(
            Long pssResolutionCodeId, Long resolutionCodeId, Long rcId, String changeType)
            implements DiffReportCsvRow {
        @Override
        public Long sortId() {
            return rcId;
        }

        @Override
        public String toCsvRow() {
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
