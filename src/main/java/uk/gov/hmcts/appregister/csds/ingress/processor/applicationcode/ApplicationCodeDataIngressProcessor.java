package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcIngressBackupService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@Slf4j
@Component
public class ApplicationCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ApplicationCodeDiffResult> {
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
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
    private final Clock clock;

    public ApplicationCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            JdbcIngressBackupService ingressBackupService,
            ApplicationCodeDiffService diffService,
            ApplicationCodeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            ApplicationCodeIngressDatabaseRowMapper rowMapper,
            Clock clock) {
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
        this.clock = clock;
    }

    @Override
    public List<JsonNode> preProcess(List<JsonNode> rawJson) {
        if (rawJson.isEmpty()) {
            return rawJson;
        }

        val today = LocalDate.now(clock.withZone(ZoneId.of("Europe/London")));
        val resolvedRecords =
                rawJson.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .filter(
                                record -> {
                                    val startDate = requiredLocalDate(record, "StartDate");
                                    if (record.hasNonNull("EndDate")) {
                                        requiredLocalDate(record, "EndDate");
                                    }
                                    if (startDate.isAfter(today)) {
                                        log.warn(
                                                "Dropping future-dated application code {}"
                                                        + " with StartDate {} (today {})",
                                                record.path("Code").asText(),
                                                startDate,
                                                today);
                                        return false;
                                    }
                                    return true;
                                })
                        .toList();
        if (!resolvedRecords.isEmpty()) {
            validateExpectedFields(resolvedRecords.getFirst(), REQUIRED_RECORD_FIELDS);
        }
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
    protected String dataLocationName() {
        return "APPREGISTER";
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
                targetTable(),
                targetKeyFields(),
                rows,
                rowMapper,
                ApplicationCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, ApplicationCodeDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData, diff),
                ApplicationCodeIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            ApplicationCodeDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData, diff),
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
                nullableLong(node, "ApplicationCodeID"),
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

    private Map<Long, JsonNode> sourceRecordsById(
            List<JsonNode> processedData, ApplicationCodeDiffResult diff) {
        var sourcesByCode =
                processedData.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .collect(
                                Collectors.toMap(node -> requiredText(node, "Code"), node -> node));
        // Audit keys remain numeric AC_IDs; ingress identity is the application code.
        return diff.diffRecords().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.intended().id(),
                                item -> sourcesByCode.get(item.intended().code())));
    }
}
