package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
import uk.gov.hmcts.appregister.csds.ingress.database.FeeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Slf4j
@Component
public class FeeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, FeeDiffResult> {
    private static final long DEFAULT_FEE_VERSION = 1L;
    private static final String FEE_ID = "FEE_ID";
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "CivilFeeID",
                    "FeeReference",
                    "Description",
                    "FeeValue",
                    "StartDate",
                    "EndDate",
                    "RevisionNumber",
                    "PSSFixedListID");

    private final CsdsIngressProperties.Fee feeProperties;
    private final FeeDiffService diffService;
    private final FeeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final FeeIngressDatabaseRowMapper rowMapper;

    public FeeDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            FeeDiffService diffService,
            FeeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            FeeIngressDatabaseRowMapper rowMapper) {
        super(
                properties,
                properties.getProcessors().getFee(),
                csdsAuditService,
                csdsIngressTransactionRunner);
        this.feeProperties = properties.getProcessors().getFee();
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
                        .map(this::withResolvedFeeId)
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val recordsArray = normalisedPage.putArray("records");
        resolvedRecords.forEach(recordsArray::add);
        return List.of(normalisedPage);
    }

    @Override
    protected String queryParameters() {
        return feeProperties.getParameters();
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
                targetTable(), targetKeyField(), rows, rowMapper, FeeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, FeeDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(), sourceRecordsById(processedData), FeeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData, FeeDiffResult diff, CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
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
                requiredLong(node, FEE_ID),
                requiredText(node, "FeeReference"),
                requiredText(node, "Description"),
                requiredBigDecimal(node, "FeeValue"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                resolvedVersion(node));
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

    private JsonNode withResolvedFeeId(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return node;
        }

        val copiedRecord = objectNode.deepCopy();
        val resolvedId = FeeIngressRecord.resolveId(copiedRecord);
        if (resolvedId != null) {
            copiedRecord.put(FEE_ID, resolvedId);
        }
        return copiedRecord;
    }

    private Map<Long, JsonNode> sourceRecordsById(List<JsonNode> processedData) {
        return indexSourceRecords(processedData, node -> nullableLong(node, FEE_ID));
    }
}
