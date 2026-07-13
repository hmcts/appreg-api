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
    private static final String APPLICANT_ID = "ApplicantID";
    private static final String PSSSA_ID = "PSSSAID";
    private static final String SA_ID = "SA_ID";
    private static final String CODE = "Code";
    private static final String NAME = "OrganisationName";
    private static final String START_DATE = "StartDate";
    private static final String END_DATE = "Enddate";
    private static final String VERSION = "RevisionNumber";
    private static final String CSV_HEADER =
            "psssaId,applicantId,saId,code,name,startDate,endDate,version\n";
    private static final String DIFF_REPORT_HEADER = "psssaId,applicantId,saId,changeType\n";
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
                        .filter(item -> nullableLong(item, SA_ID) != null)
                        .collect(
                                Collectors.toMap(
                                        item -> nullableLong(item, SA_ID),
                                        Function.identity(),
                                        (first, second) -> second));
        return diffRecords.stream()
                .<DiffReportCsvRow>map(
                        item -> {
                            var source = incomingRecordsBySaId.get(item.intended().id());
                            return new DiffReportRow(
                                    nullableLong(source, PSSSA_ID),
                                    nullableLong(source, APPLICANT_ID),
                                    item.intended().id(),
                                    changeType(item.operation()));
                        })
                .toList();
    }

    @Override
    protected String buildExistingCsv(Map<Long, StandardApplicantIngressRecord> existingById) {
        return buildCsv(
                CSV_HEADER,
                existingById.values().stream()
                        .sorted(Comparator.comparing(StandardApplicantIngressRecord::id))
                        .map(this::toExistingCsvRow));
    }

    @Override
    protected String buildIncomingCsv(
            List<JsonNode> processedData, Function<JsonNode, List<JsonNode>> recordsExtractor) {
        return buildCsv(
                CSV_HEADER,
                processedData.stream()
                        .flatMap(page -> recordsExtractor.apply(page).stream())
                        .map(this::toIncomingCsvRow));
    }

    @Override
    protected String diffReportHeader() {
        return DIFF_REPORT_HEADER;
    }

    private String toIncomingCsvRow(JsonNode node) {
        return String.join(
                        ",",
                        csvValue(nullableLong(node, PSSSA_ID)),
                        csvValue(nullableLong(node, APPLICANT_ID)),
                        csvValue(nullableLong(node, SA_ID)),
                        csvValue(nullableText(node, CODE)),
                        csvValue(nullableText(node, NAME)),
                        csvValue(nullableText(node, START_DATE)),
                        csvValue(nullableText(node, END_DATE)),
                        csvValue(nullableLong(node, VERSION)))
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
