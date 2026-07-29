package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
public class FeeDiffReportingService extends AbstractIngressDiffReportingService<FeeIngressRecord> {
    private static final long DEFAULT_FEE_VERSION = 1L;
    private static final String FEE_ID = "FEE_ID";
    private static final String CSV_HEADER =
            "pssFixedListId,civilFeeId,feeId,reference,description,amount,startDate,endDate,version\n";
    private final String reportingDir;

    public FeeDiffReportingService(CsdsIngressProperties properties) {
        this.reportingDir = properties.getProcessors().getFee().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            FeeDiffResult diffResult,
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
        return "fee";
    }

    @Override
    protected List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<IngressDiffRecord<FeeIngressRecord, FeeIngressRecord, FeeIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var incomingRecordsByFeeId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, FEE_ID) != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, FEE_ID),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
                        item ->
                                new DiffReportRow(
                                        nullableLong(
                                                incomingRecordsByFeeId.get(item.intended().id()),
                                                "PSSFixedListID"),
                                        nullableLong(
                                                incomingRecordsByFeeId.get(item.intended().id()),
                                                "CivilFeeID"),
                                        item.intended().id(),
                                        changeType(item.operation())))
                .toList();
    }

    @Override
    protected String buildExistingCsv(Map<Long, FeeIngressRecord> existingById) {
        var csv = new StringBuilder(CSV_HEADER);
        existingById.values().stream()
                .sorted(Comparator.comparing(FeeIngressRecord::id))
                .map(this::toExistingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    @Override
    protected String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var csv = new StringBuilder(CSV_HEADER);
        processedData.stream()
                .flatMap(page -> recordsExtractor.apply(page).stream())
                .map(this::toIncomingCsvRow)
                .forEach(csv::append);
        return csv.toString();
    }

    @Override
    protected String diffReportHeader() {
        return "pssFixedListId,civilFeeId,feeId,changeType\n";
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSFixedListID")),
                        csvValue(nullableLong(node, "CivilFeeID")),
                        csvValue(nullableLong(node, FEE_ID)),
                        csvValue(nullableText(node, "FeeReference")),
                        csvValue(nullableText(node, "Description")),
                        csvValue(nullableText(node, "FeeValue")),
                        csvValue(nullableText(node, "StartDate")),
                        csvValue(nullableText(node, "EndDate")),
                        csvValue(resolvedVersion(node)))
                + "\n";
    }

    private Long resolvedVersion(JsonNode node) {
        var revisionNumber = nullableLong(node, "RevisionNumber");
        if (revisionNumber != null) {
            return revisionNumber;
        }

        var versionNumber = nullableLong(node, "VersionNumber");
        if (versionNumber != null) {
            return versionNumber;
        }

        return DEFAULT_FEE_VERSION;
    }

    private String toExistingCsvRow(FeeIngressRecord item) {
        return String.join(
                        ",",
                        csvValue((Object) null),
                        csvValue((Object) null),
                        csvValue(item.id()),
                        csvValue(item.reference()),
                        csvValue(item.description()),
                        csvValue(item.amount()),
                        csvValue(item.startDate()),
                        csvValue(item.endDate()),
                        csvValue(item.version()))
                + "\n";
    }

    private record DiffReportRow(
            Long pssFixedListId, Long civilFeeId, Long feeId, String changeType)
            implements DiffReportCsvRow {
        @Override
        public Long sortId() {
            return feeId;
        }

        @Override
        public String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(pssFixedListId),
                            csvValue(civilFeeId),
                            csvValue(feeId),
                            csvValue(changeType))
                    + "\n";
        }
    }
}
