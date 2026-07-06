package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.database.ApplicationCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.processors.application-codes",
        name = "enabled",
        havingValue = "true")
public class ApplicationCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ApplicationCodeDiffResult> {

    private final CsdsIngressProperties.ApplicationCodes applicationCodeProperties;
    private final ApplicationCodeDiffService diffService;
    private final ApplicationCodeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final ApplicationCodeIngressDatabaseRowMapper rowMapper;

    public ApplicationCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            ApplicationCodeDiffService diffService,
            ApplicationCodeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            ApplicationCodeIngressDatabaseRowMapper rowMapper) {
        super(properties, properties.getProcessors().getApplicationCodes());
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

        val sortedRecords =
                rawJson.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .map(this::withResolvedAcId)
                        .sorted(
                                Comparator.comparing(
                                        this::applicationCodeIdForSort,
                                        Comparator.nullsLast(Long::compareTo)))
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val sortedArray = normalisedPage.putArray("records");
        sortedRecords.forEach(sortedArray::add);
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
                targetTable(), processedData, this::toSourceRecord, this::extractRecords);
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
        val rows =
                diff.diffRecords().stream()
                        .filter(item -> item.operation() != IngressOperation.IGNORE)
                        .map(IngressDiffRecord::intended)
                        .toList();
        bulkUpsertService.upsertBatch(targetTable(), targetKeyField(), rows, rowMapper);
    }

    private ApplicationCodeIngressRecord toSourceRecord(JsonNode node) {
        return new ApplicationCodeIngressRecord(
                requiredLong(node, "AC_ID"),
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
        val resolvedId =
                ApplicationCodeIngressRecord.calculateId(
                        nullableLong(copiedRecord, "PSSApplicationCodeID"),
                        nullableLong(copiedRecord, "ApplicationCodeID"));
        if (resolvedId != null) {
            copiedRecord.put("AC_ID", resolvedId);
        }
        return copiedRecord;
    }

    private Long applicationCodeIdForSort(JsonNode node) {
        return nullableLong(node, "ApplicationCodeID");
    }
}
