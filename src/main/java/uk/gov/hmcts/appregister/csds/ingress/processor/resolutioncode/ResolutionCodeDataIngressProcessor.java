package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

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
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Slf4j
@Component
public class ResolutionCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ResolutionCodeDiffResult> {
    private static final String RC_ID = "RC_ID";
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "ResolutionCodeID",
                    "PSSRCID",
                    "Code",
                    "ResultTitle",
                    "ResultWording",
                    "Legislation",
                    "Recipient1Email",
                    "Recipient2Email",
                    "StartDate",
                    "EndDate",
                    "RevisionNumber");

    private final CsdsIngressProperties.ResolutionCodes resolutionCodeProperties;
    private final ResolutionCodeDiffService diffService;
    private final ResolutionCodeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final ResolutionCodeIngressDatabaseRowMapper rowMapper;

    public ResolutionCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            ResolutionCodeDiffService diffService,
            ResolutionCodeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            ResolutionCodeIngressDatabaseRowMapper rowMapper) {
        super(
                properties,
                properties.getProcessors().getResolutionCodes(),
                csdsAuditService,
                csdsIngressTransactionRunner);
        resolutionCodeProperties = properties.getProcessors().getResolutionCodes();
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
                        .map(this::withResolvedRcId)
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val recordsArray = normalisedPage.putArray("records");
        resolvedRecords.forEach(recordsArray::add);
        return List.of(normalisedPage);
    }

    @Override
    protected String queryParameters() {
        return resolutionCodeProperties.getParameters();
    }

    @Override
    protected String mockFilePath() {
        return resolutionCodeProperties.getMock();
    }

    @Override
    protected ResolutionCodeDiffResult diff(List<JsonNode> processedData) {
        return diffService.diff(
                new ResolutionCodeDiffRequest(
                        targetTable(), processedData, this::toSourceRecord, this::extractRecords));
    }

    @Override
    protected void logDiffSummary(ResolutionCodeDiffResult diff) {
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
    protected void report(List<JsonNode> processedData, ResolutionCodeDiffResult diff) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                diff,
                this::extractRecords);
    }

    @Override
    protected void applyDiff(ResolutionCodeDiffResult diff) {
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(
                targetTable(), targetKeyField(), rows, rowMapper, ResolutionCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, ResolutionCodeDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                ResolutionCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            ResolutionCodeDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                ResolutionCodeIngressRecord::id,
                ResolutionCodeIngressRecord.class,
                ex);
    }

    @Override
    public String processorName() {
        return CsdsIngestProcessorName.RESOLUTION_CODES.getExternalName();
    }

    @Override
    public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
        val processedData = preProcess(rawJson);
        val diff = applyWithAuditing(processedData);

        var insertedCount = countByOperation(diff, IngressOperation.INSERT);
        var updatedCount = countByOperation(diff, IngressOperation.UPDATE);

        return new CsdsIngestResponse().inserted(insertedCount).updated(updatedCount);
    }

    private int countByOperation(ResolutionCodeDiffResult diff, IngressOperation operation) {
        return Math.toIntExact(
                diff.diffRecords().stream().filter(item -> item.operation() == operation).count());
    }

    private ResolutionCodeIngressRecord toSourceRecord(JsonNode node) {
        return new ResolutionCodeIngressRecord(
                requiredLong(node, RC_ID),
                requiredText(node, "Code"),
                requiredText(node, "ResultTitle"),
                requiredText(node, "ResultWording"),
                nullableText(node, "Legislation"),
                nullableText(node, "Recipient1Email"),
                nullableText(node, "Recipient2Email"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                requiredLong(node, "RevisionNumber"));
    }

    private JsonNode withResolvedRcId(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return node;
        }

        val copiedRecord = objectNode.deepCopy();
        val resolvedId = ResolutionCodeIngressRecord.resolveId(copiedRecord);
        if (resolvedId != null) {
            copiedRecord.put(RC_ID, resolvedId);
        }
        return copiedRecord;
    }

    private Map<Long, JsonNode> sourceRecordsById(List<JsonNode> processedData) {
        return indexSourceRecords(processedData, node -> nullableLong(node, RC_ID));
    }
}
