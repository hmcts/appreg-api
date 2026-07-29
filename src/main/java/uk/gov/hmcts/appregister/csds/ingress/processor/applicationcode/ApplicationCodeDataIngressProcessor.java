package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

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
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Slf4j
@Component
public class ApplicationCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ApplicationCodeDiffResult> {
    private static final String AC_ID = "AC_ID";
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "ApplicationCodeID",
                    "PSSApplicationCodeID",
                    "Code",
                    "ApplicationTitle",
                    "ApplicationWording",
                    "Legislation",
                    "FeeDue",
                    "FeeReference",
                    "Respondent",
                    "StartDate",
                    "EndDate",
                    "BulkRespondentAllowed",
                    "RevisionNumber");

    private final CsdsIngressProperties.ApplicationCodes applicationCodeProperties;
    private final ApplicationCodeDiffService diffService;
    private final ApplicationCodeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    public ApplicationCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            JdbcIngressBackupService ingressBackupService,
            ApplicationCodeDiffService diffService,
            ApplicationCodeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            ApplicationCodeIngressDatabaseRowMapper rowMapper) {
        super(
                properties,
                properties.getProcessors().getApplicationCodes(),
                csdsAuditService,
                csdsIngressTransactionRunner,
                ingressBackupService);
        applicationCodeProperties = properties.getProcessors().getApplicationCodes();
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
                        .map(this::withResolvedAcId)
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val recordsArray = normalisedPage.putArray("records");
        resolvedRecords.forEach(recordsArray::add);
        return List.of(normalisedPage);
    }

    @Override
    protected String queryParameters() {
        return applicationCodeProperties.getParameters();
    }

    @Override
    protected String mockFilePath() {
        return applicationCodeProperties.getMock();
    }

    @Override
    protected ApplicationCodeDiffResult diff(List<JsonNode> processedData) {
        return diffService.diff(
                new ApplicationCodeDiffRequest(
                        targetTable(), processedData, this::toSourceRecord, this::extractRecords));
    }

    @Override
    protected void logDiffSummary(ApplicationCodeDiffResult diff) {
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
    protected void report(List<JsonNode> processedData, ApplicationCodeDiffResult diff) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                diff,
                this::extractRecords);
    }

    @Override
    protected void applyDiff(ApplicationCodeDiffResult diff) {
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(
                targetTable(), targetKeyField(), rows, rowMapper, ApplicationCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, ApplicationCodeDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                ApplicationCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            ApplicationCodeDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                ApplicationCodeIngressRecord::id,
                ApplicationCodeIngressRecord.class,
                ex);
    }

    @Override
    public String processorName() {
        return CsdsIngestProcessorName.APPLICATION_CODES.getExternalName();
    }

    @Override
    public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
        val processedData = preProcess(rawJson);
        val diff = applyWithAuditing(processedData);

        var insertedCount = countByOperation(diff, IngressOperation.INSERT);
        var updatedCount = countByOperation(diff, IngressOperation.UPDATE);

        return new CsdsIngestResponse().inserted(insertedCount).updated(updatedCount);
    }

    private int countByOperation(ApplicationCodeDiffResult diff, IngressOperation operation) {
        return Math.toIntExact(
                diff.diffRecords().stream().filter(item -> item.operation() == operation).count());
    }

    private ApplicationCodeIngressRecord toSourceRecord(JsonNode node) {
        return new ApplicationCodeIngressRecord(
                requiredLong(node, AC_ID),
                requiredText(node, "Code"),
                requiredText(node, "ApplicationTitle"),
                requiredText(node, "ApplicationWording"),
                nullableText(node, "Legislation"),
                requiredYesOrNo(node, "FeeDue"),
                requiredYesOrNo(node, "Respondent"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                requiredYesOrNo(node, "BulkRespondentAllowed"),
                requiredLong(node, "RevisionNumber"),
                nullableText(node, "FeeReference"));
    }

    private JsonNode withResolvedAcId(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return node;
        }

        val copiedRecord = objectNode.deepCopy();
        val resolvedId = ApplicationCodeIngressRecord.resolveId(copiedRecord);
        if (resolvedId != null) {
            copiedRecord.put(AC_ID, resolvedId);
        }
        return copiedRecord;
    }

    private Map<Long, JsonNode> sourceRecordsById(List<JsonNode> processedData) {
        return indexSourceRecords(processedData, node -> nullableLong(node, AC_ID));
    }
}
