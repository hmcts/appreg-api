package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.ResolutionCodeIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.processors.resolution-codes",
        name = "enabled",
        havingValue = "true")
public class ResolutionCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ResolutionCodeDiffResult> {
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "ResolutionCodeID",
                    "Code",
                    "ResultTitle",
                    "ResultWording",
                    "Legislation",
                    "Recipient1Email",
                    "Recipient2Email",
                    "StartDate",
                    "EndDate",
                    "Notes",
                    "AuthoringStatus",
                    "PublishingStatus",
                    "CurrentRecordIndicator",
                    "DraftFinalExistsIndicator",
                    "RevisionNumber",
                    "RevisionType",
                    "RevisionDateFrom",
                    "RevisionDateTo",
                    "ClonedFrom",
                    "PSSRCID",
                    "PSSChangeSetHeaderID",
                    "PSSChangeSetItemID",
                    "FID_ApplicationRegisterHeader",
                    "FID_ReleasePackage",
                    "Updator");

    private final CsdsIngressProperties.ResolutionCodes resolutionCodeProperties;
    private final ResolutionCodeDiffService diffService;
    private final ResolutionCodeDiffReportingService diffReportingService;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final ResolutionCodeIngressDatabaseRowMapper rowMapper;

    public ResolutionCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            ResolutionCodeDiffService diffService,
            ResolutionCodeDiffReportingService diffReportingService,
            JdbcBulkUpsertService bulkUpsertService,
            ResolutionCodeIngressDatabaseRowMapper rowMapper) {
        super(properties, properties.getProcessors().getResolutionCodes());
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

        val sortedRecords =
                rawJson.stream()
                        .flatMap(page -> extractRecords(page).stream())
                        .map(this::withResolvedRcId)
                        .sorted(
                                Comparator.comparing(
                                        this::resolutionCodeIdForSort,
                                        Comparator.nullsLast(Long::compareTo)))
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val sortedArray = normalisedPage.putArray("records");
        sortedRecords.forEach(sortedArray::add);
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
        val rows =
                diff.diffRecords().stream()
                        .filter(item -> item.operation() != IngressOperation.IGNORE)
                        .map(IngressDiffRecord::intended)
                        .toList();
        bulkUpsertService.upsertBatch(targetTable(), targetKeyField(), rows, rowMapper);
    }

    private ResolutionCodeIngressRecord toSourceRecord(JsonNode node) {
        return new ResolutionCodeIngressRecord(
                requiredLong(node, "RC_ID"),
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
            copiedRecord.put("RC_ID", resolvedId);
        }
        return copiedRecord;
    }

    private Long resolutionCodeIdForSort(JsonNode node) {
        return nullableLong(node, "ResolutionCodeID");
    }
}
