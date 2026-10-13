package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestResponse;
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

@Slf4j
@Component
public class NationalCourtHouseDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, NationalCourtHouseDiffResult> {
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of("CourtWelshName", "CourtName", "StartDate", "EndDate", "RevisionNumber");

    private final CsdsIngressProperties.NationalCourtHouses nationalCourtHouseProperties;
    private final NationalCourtHouseDiffService diffService;
    private final NationalCourtHouseDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final NationalCourtHouseIngressDatabaseRowMapper rowMapper;
    private final Clock clock;

    public NationalCourtHouseDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            JdbcIngressBackupService ingressBackupService,
            NationalCourtHouseDiffService diffService,
            NationalCourtHouseDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            NationalCourtHouseIngressDatabaseRowMapper rowMapper,
            Clock clock) {
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
        this.clock = clock;
    }

    @Override
    public List<JsonNode> preProcess(List<JsonNode> rawJson) {
        return normaliseDateEffectiveRecords(
                rawJson,
                clock,
                REQUIRED_RECORD_FIELDS,
                "national courthouse",
                record ->
                        nullableText(
                                record, nationalCourtHouseProperties.getBusinessKeys().getFirst()),
                "StartDate");
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
                targetKeyFields(),
                rows,
                rowMapper,
                NationalCourtHouseIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, NationalCourtHouseDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData, diff),
                NationalCourtHouseIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            NationalCourtHouseDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData, diff),
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
        val codeField = nationalCourtHouseProperties.getBusinessKeys().getFirst();
        val code = requiredText(node, codeField);
        if (code.isBlank()) {
            throw invalidField(node, codeField);
        }
        return new NationalCourtHouseIngressRecord(
                nullableLong(node, nationalCourtHouseProperties.getCsdsKey()),
                requiredText(node, "CourtName"),
                requiredLong(node, "RevisionNumber"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                code,
                nullableText(node, "CourtWelshName"),
                nullableLong(node, nationalCourtHouseProperties.getLegacyKey()));
    }

    private Map<Long, JsonNode> sourceRecordsById(
            List<JsonNode> processedData, NationalCourtHouseDiffResult diff) {
        val sourcesByCode =
                processedData.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .collect(
                                Collectors.toMap(
                                        node ->
                                                requiredText(
                                                        node,
                                                        nationalCourtHouseProperties
                                                                .getBusinessKeys()
                                                                .getFirst()),
                                        node -> node));
        // Audit IDs stay numeric while raw source records are matched by location code.
        return diff.diffRecords().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.intended().id(),
                                item -> sourcesByCode.get(item.intended().courtLocationCode())));
    }
}
