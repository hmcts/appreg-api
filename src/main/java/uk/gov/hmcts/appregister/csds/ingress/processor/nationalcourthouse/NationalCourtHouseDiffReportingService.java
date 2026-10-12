package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class NationalCourtHouseDiffReportingService
        extends AbstractIngressDiffReportingService<NationalCourtHouseIngressRecord> {
    private static final String NCH_ID = "NCH_ID";

    private final CsdsIngressProperties.NationalCourtHouses processorProperties;

    public NationalCourtHouseDiffReportingService(CsdsIngressProperties properties) {
        processorProperties = properties.getProcessors().getNationalCourtHouses();
    }

    public void reportDiff(
            String datasetName,
            String targetTable,
            String targetKeyField,
            List<JsonNode> processedData,
            NationalCourtHouseDiffResult diffResult,
            Function<JsonNode, List<JsonNode>> recordsExtractor) {
        var idsByCode =
                diffResult.incomingById().values().stream()
                        .collect(
                                Collectors.toMap(
                                        NationalCourtHouseIngressRecord::courtLocationCode,
                                        NationalCourtHouseIngressRecord::id));
        // Decorate report copies only: the CSV retains resolved IDs, while source/audit JSON is
        // untouched.
        var reportPages =
                processedData.stream()
                        .map(
                                page -> {
                                    JsonNode copy = page.deepCopy();
                                    recordsExtractor
                                            .apply(copy)
                                            .forEach(
                                                    record ->
                                                            ((ObjectNode) record)
                                                                    .put(
                                                                            NCH_ID,
                                                                            idsByCode.get(
                                                                                    nullableText(
                                                                                            record,
                                                                                            processorProperties
                                                                                                    .getBusinessKeys()
                                                                                                    .getFirst()))));
                                    return copy;
                                })
                        .toList();
        super.reportDiff(
                processorProperties.getReportingDir(),
                datasetName,
                targetTable,
                targetKeyField,
                reportPages,
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
        var incomingRecordsByCode =
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
                                                incomingRecordsByCode.get(
                                                        item.intended().courtLocationCode()),
                                                processorProperties.getLegacyKey()),
                                        nullableLong(
                                                incomingRecordsByCode.get(
                                                        item.intended().courtLocationCode()),
                                                processorProperties.getCsdsKey()),
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
                        csvValue(nullableLong(node, processorProperties.getLegacyKey())),
                        csvValue(nullableLong(node, processorProperties.getCsdsKey())),
                        csvValue(nullableLong(node, NCH_ID)),
                        csvValue(nullableText(node, "CourtName")),
                        csvValue(nullableText(node, "CourtWelshName")),
                        csvValue(
                                nullableText(
                                        node, processorProperties.getBusinessKeys().getFirst())),
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
