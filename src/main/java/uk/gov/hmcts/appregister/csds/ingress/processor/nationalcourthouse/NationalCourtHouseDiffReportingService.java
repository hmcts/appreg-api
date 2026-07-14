package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

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
public class NationalCourtHouseDiffReportingService
        extends AbstractIngressDiffReportingService<NationalCourtHouseIngressRecord> {
    private static final String NCH_ID = "NCH_ID";

    private final String reportingDir;

    public NationalCourtHouseDiffReportingService(CsdsIngressProperties properties) {
        reportingDir = properties.getProcessors().getNationalCourtHouses().getReportingDir();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            NationalCourtHouseDiffResult diffResult,
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
        return "national_court_houses";
    }

    @Override
    protected List<DiffReportCsvRow> buildDiffReport(
            List<JsonNode> processedData,
            List<
                            IngressDiffRecord<
                                    NationalCourtHouseIngressRecord,
                                    NationalCourtHouseIngressRecord,
                                    NationalCourtHouseIngressRecord>>
                    diffRecords,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var incomingRecordsByNchId =
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .filter(item -> nullableLong(item, NCH_ID) != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, NCH_ID),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
                        item ->
                                new DiffReportRow(
                                        nullableLong(
                                                incomingRecordsByNchId.get(item.intended().id()),
                                                "PSSNationalCourtHouseID"),
                                        nullableLong(
                                                incomingRecordsByNchId.get(item.intended().id()),
                                                "CourtID"),
                                        item.intended().id(),
                                        changeType(item.operation())))
                .toList();
    }

    @Override
    protected String buildExistingCsv(Map<Long, NationalCourtHouseIngressRecord> existingById) {
        return buildCsv(
                csvHeader(),
                existingById.values().stream()
                        .sorted(Comparator.comparing(NationalCourtHouseIngressRecord::id))
                        .map(this::toExistingCsvRow));
    }

    @Override
    protected String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        return buildCsv(
                csvHeader(),
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .map(this::toIncomingCsvRow));
    }

    private String csvHeader() {
        return "pssNationalCourtHouseId,courtId,nchId,name,welshName,courtLocationCode,"
                + "startDate,endDate,version\n";
    }

    @Override
    protected String diffReportHeader() {
        return "pssNationalCourtHouseId,courtId,nchId,changeType\n";
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, "PSSNationalCourtHouseID")),
                        csvValue(nullableLong(node, "CourtID")),
                        csvValue(nullableLong(node, NCH_ID)),
                        csvValue(nullableText(node, "CourtName")),
                        csvValue(nullableText(node, "CourtWelshName")),
                        csvValue(nullableText(node, "CourtLocationCode")),
                        csvValue(nullableText(node, "StartDate")),
                        csvValue(nullableText(node, "EndDate")),
                        csvValue(nullableLong(node, "RevisionNumber")))
                + "\n";
    }

    private String toExistingCsvRow(NationalCourtHouseIngressRecord item) {
        return String.join(
                        ",",
                        csvValue((Object) null),
                        csvValue((Object) null),
                        csvValue(item.id()),
                        csvValue(item.name()),
                        csvValue(item.welshName()),
                        csvValue(item.courtLocationCode()),
                        csvValue(item.startDate()),
                        csvValue(item.endDate()),
                        csvValue(item.version()))
                + "\n";
    }

    private record DiffReportRow(
            Long pssNationalCourtHouseId, Long courtId, Long nchId, String changeType)
            implements DiffReportCsvRow {
        @Override
        public Long sortId() {
            return nchId;
        }

        @Override
        public String toCsvRow() {
            return String.join(
                            ",",
                            csvValue(pssNationalCourtHouseId),
                            csvValue(courtId),
                            csvValue(nchId),
                            csvValue(changeType))
                    + "\n";
        }
    }
}
