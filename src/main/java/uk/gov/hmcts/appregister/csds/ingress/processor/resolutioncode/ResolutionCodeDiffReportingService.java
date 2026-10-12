package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractIngressDiffReportingService;

@Service
public class ResolutionCodeDiffReportingService
        extends AbstractIngressDiffReportingService<ResolutionCodeIngressRecord> {
    private static final String RC_ID = "RC_ID";
    private final CsdsIngressProperties.ResolutionCodes processorProperties;

    public ResolutionCodeDiffReportingService(CsdsIngressProperties properties) {
        this.processorProperties = properties.getProcessors().getResolutionCodes();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            ResolutionCodeDiffResult diffResult,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        super.reportDiff(
                processorProperties.getReportingDir(),
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
                        .collect(
                                Collectors.toMap(
                                        item ->
                                                nullableText(
                                                        item,
                                                        processorProperties
                                                                .getBusinessKeys()
                                                                .getFirst()),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
                        item ->
                                new DiffReportRow(
                                        nullableLong(
                                                incomingRecordsByRcId.get(item.intended().code()),
                                                processorProperties.getLegacyKey()),
                                        nullableLong(
                                                incomingRecordsByRcId.get(item.intended().code()),
                                                processorProperties.getCsdsKey()),
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
                        csvValue(nullableLong(node, processorProperties.getLegacyKey())),
                        csvValue(nullableLong(node, processorProperties.getCsdsKey())),
                        csvValue(nullableLong(node, RC_ID)),
                        csvValue(
                                nullableText(
                                        node, processorProperties.getBusinessKeys().getFirst())),
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
