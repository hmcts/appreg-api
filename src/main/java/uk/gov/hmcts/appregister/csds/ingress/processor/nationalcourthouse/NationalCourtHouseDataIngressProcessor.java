package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditEntry;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.database.NationalCourtHouseIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Slf4j
@Component
public class NationalCourtHouseDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, NationalCourtHouseDiffResult> {
    private static final String NCH_ID = "NCH_ID";
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "CourtID",
                    "PSSNationalCourtHouseID",
                    "CourtName",
                    "CourtWelshName",
                    "CourtLocationCode",
                    "StartDate",
                    "EndDate",
                    "RevisionNumber");

    private final CsdsIngressProperties.NationalCourtHouses nationalCourtHouseProperties;
    private final NationalCourtHouseDiffService diffService;
    private final NationalCourtHouseDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;

    public NationalCourtHouseDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            JdbcIngressBackupService ingressBackupService,
            NationalCourtHouseDiffService diffService,
            NationalCourtHouseDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            NationalCourtHouseIngressDatabaseRowMapper rowMapper) {
        super(
                properties,
                properties.getProcessors().getNationalCourtHouses(),
                csdsAuditService,
                csdsIngressTransactionRunner,
                ingressBackupService);
        nationalCourtHouseProperties = properties.getProcessors().getNationalCourtHouses();
        this.diffService = diffService;
        this.diffReportingService = diffReportingService;
        this.bulkUpsertService = bulkUpsertService;
        this.rowMapper = rowMapper;
    }

    @Override
    public List<JsonNode> preProcess(List<JsonNode> rawJson) {
        if (rawJson.isEmpty()) {
            return rawJson;
        }

        val firstPageRecords = extractRecords(rawJson.getFirst());
        if (!firstPageRecords.isEmpty()) {
            validateExpectedFields(firstPageRecords.getFirst(), REQUIRED_RECORD_FIELDS);
        }

        val resolvedRecords =
                rawJson.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .map(this::withResolvedNchId)
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val recordsArray = normalisedPage.putArray("records");
        resolvedRecords.forEach(recordsArray::add);
        return List.of(normalisedPage);
    }

    @Override
    protected String queryParameters() {
        return nationalCourtHouseProperties.getParameters();
    }

    @Override
    protected String dataLocationName() {
        return "COURT";
    }

    @Override
    protected String mockFilePath() {
        return nationalCourtHouseProperties.getMock();
    }

    @Override
    protected NationalCourtHouseDiffResult diff(List<JsonNode> processedData) {
        return diffService.diff(
                new NationalCourtHouseDiffRequest(
                        targetTable(), processedData, this::toSourceRecord, this::extractRecords));
    }

    @Override
    protected void logDiffSummary(NationalCourtHouseDiffResult diff) {
        log.info(
                "CSDS ingress processor {} produced inserts={}, updates={}",
                datasetName(),
                countByOperation(diff, IngressOperation.INSERT),
                countByOperation(diff, IngressOperation.UPDATE));
    }

    @Override
    protected void report(List<JsonNode> processedData, NationalCourtHouseDiffResult diff) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                diff,
                this::extractRecords);
    }

    @Override
    protected void applyDiff(NationalCourtHouseDiffResult diff) {
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(
                targetTable(),
                targetKeyField(),
                rows,
                rowMapper,
                NationalCourtHouseIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, NationalCourtHouseDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                NationalCourtHouseIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            NationalCourtHouseDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                NationalCourtHouseIngressRecord::id,
                NationalCourtHouseIngressRecord.class,
                ex);
    }

    @Override
    public String processorName() {
        return CsdsIngestProcessorName.NATIONAL_COURT_HOUSES.getExternalName();
    }

    @Override
    public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
        val processedData = preProcess(rawJson);
        val diff = applyWithAuditing(processedData);
        return new CsdsIngestResponse()
                .inserted(countByOperation(diff, IngressOperation.INSERT))
                .updated(countByOperation(diff, IngressOperation.UPDATE));
    }

    private int countByOperation(NationalCourtHouseDiffResult diff, IngressOperation operation) {
        return Math.toIntExact(
                diff.diffRecords().stream().filter(item -> item.operation() == operation).count());
    }

    private NationalCourtHouseIngressRecord toSourceRecord(JsonNode node) {
        return new NationalCourtHouseIngressRecord(
                requiredLong(node, NCH_ID),
                requiredText(node, "CourtName"),
                requiredLong(node, "RevisionNumber"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                nullableText(node, "CourtLocationCode"),
                nullableText(node, "CourtWelshName"));
    }

    private JsonNode withResolvedNchId(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return node;
        }
        val copiedRecord = objectNode.deepCopy();
        copiedRecord.put(NCH_ID, NationalCourtHouseIngressRecord.resolveId(copiedRecord));
        return copiedRecord;
    }

    private Map<Long, JsonNode> sourceRecordsById(List<JsonNode> processedData) {
        return indexSourceRecords(processedData, node -> nullableLong(node, NCH_ID));
    }
}
