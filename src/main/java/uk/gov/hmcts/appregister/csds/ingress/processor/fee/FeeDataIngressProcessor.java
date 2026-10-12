package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@Slf4j
@Component
public class FeeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, FeeDiffResult> {
    private static final long DEFAULT_FEE_VERSION = 1L;
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "FeeReference",
                    "Description",
                    "FeeValue",
                    "StartDate",
                    "EndDate",
                    "RevisionNumber");

    private final CsdsIngressProperties.Fee feeProperties;
    private final FeeDiffService diffService;
    private final FeeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final FeeIngressDatabaseRowMapper rowMapper;
    private final Clock clock;

    public FeeDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            JdbcIngressBackupService ingressBackupService,
            FeeDiffService diffService,
            FeeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            FeeIngressDatabaseRowMapper rowMapper,
            Clock clock) {
        super(
                properties,
                properties.getProcessors().getFee(),
                csdsAuditService,
                csdsIngressTransactionRunner,
                ingressBackupService);
        this.feeProperties = properties.getProcessors().getFee();
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
                "fee",
                record -> nullableText(record, "FeeReference"));
    }

    @Override
    protected String queryParameters() {
        return feeProperties.getParameters();
    }

    @Override
    protected String dataLocationName() {
        return "APPREGISTER";
    }

    @Override
    protected String mockFilePath() {
        return feeProperties.getMock();
    }

    @Override
    protected FeeDiffResult diff(List<JsonNode> processedData) {
        return diffService.diff(
                new FeeDiffRequest(
                        targetTable(), processedData, this::toSourceRecord, this::extractRecords));
    }

    @Override
    protected void logDiffSummary(FeeDiffResult diff) {
        val insertCount =
                diff.diffRecords().stream()
                        .filter(item -> item.operation() == IngressOperation.INSERT)
                        .count();
        val updateCount =
                diff.diffRecords().stream()
                        .filter(item -> item.operation() == IngressOperation.UPDATE)
                        .count();
        log.info(
                "CSDS ingress processor {} produced inserts={}, updates={}",
                datasetName(),
                insertCount,
                updateCount);
    }

    @Override
    protected void report(List<JsonNode> processedData, FeeDiffResult diff) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                diff,
                this::extractRecords);
    }

    @Override
    protected void applyDiff(FeeDiffResult diff) {
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(
                targetTable(), targetKeyFields(), rows, rowMapper, FeeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, FeeDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(), sourceRecordsById(processedData, diff), FeeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData, FeeDiffResult diff, CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData, diff),
                FeeIngressRecord::id,
                FeeIngressRecord.class,
                ex);
    }

    @Override
    public String processorName() {
        return CsdsIngestProcessorName.FEE.getExternalName();
    }

    @Override
    public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
        val processedData = preProcess(rawJson);
        val diff = applyWithAuditing(processedData);

        var insertedCount = countByOperation(diff, IngressOperation.INSERT);
        var updatedCount = countByOperation(diff, IngressOperation.UPDATE);

        return new CsdsIngestResponse().inserted(insertedCount).updated(updatedCount);
    }

    private int countByOperation(FeeDiffResult diff, IngressOperation operation) {
        return Math.toIntExact(
                diff.diffRecords().stream().filter(item -> item.operation() == operation).count());
    }

    private FeeIngressRecord toSourceRecord(JsonNode node) {
        return new FeeIngressRecord(
                nullableLong(node, "CivilFeeID"),
                requiredText(node, "FeeReference"),
                requiredText(node, "Description"),
                requiredBigDecimal(node, "FeeValue"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                resolvedVersion(node),
                nullableLong(node, "PSSFixedListID"));
    }

    private Long resolvedVersion(JsonNode node) {
        val revisionNumber = nullableLong(node, "RevisionNumber");
        if (revisionNumber != null) {
            return revisionNumber;
        }

        val versionNumber = nullableLong(node, "VersionNumber");
        if (versionNumber != null) {
            return versionNumber;
        }

        // CSDS currently returns null fee revision/version values for some live records.
        return DEFAULT_FEE_VERSION;
    }

    private Map<Long, JsonNode> sourceRecordsById(
            List<JsonNode> processedData, FeeDiffResult diff) {
        val sourcesByKey =
                processedData.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .collect(
                                Collectors.toMap(
                                        node ->
                                                new FeeIngressRecord.BusinessKey(
                                                        requiredText(node, "FeeReference"),
                                                        requiredLocalDate(node, "StartDate")),
                                        node -> node));
        // Audit IDs stay numeric; source correlation uses the complete business key.
        return diff.diffRecords().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.intended().id(),
                                item -> sourcesByKey.get(item.intended().businessKey())));
    }
}
