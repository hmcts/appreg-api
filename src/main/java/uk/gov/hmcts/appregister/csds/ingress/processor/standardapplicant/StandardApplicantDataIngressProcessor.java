package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngestProcessorName;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditEntry;
import uk.gov.hmcts.appregister.csds.ingress.audit.CsdsAuditService;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsBatchUpsertException;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressOperation;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;
import uk.gov.hmcts.appregister.generated.model.CsdsIngestResponse;

@Slf4j
@Component
public class StandardApplicantDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, StandardApplicantDiffResult> {
    private static final String MISSING_ADDRESS = "<missing>";
    private static final String SA_ID = "SA_ID";
    private static final List<String> REQUIRED_RECORD_FIELDS =
            List.of(
                    "ApplicantID",
                    "PSSSAID",
                    "Code",
                    "OrganisationName",
                    "StartDate",
                    "Enddate",
                    "RevisionNumber",
                    "Address",
                    "ContactInformation");

    private final CsdsIngressProperties.StandardApplicants standardApplicantProperties;
    private final StandardApplicantDiffService diffService;
    private final StandardApplicantDiffReportingService diffReportingService;
    private final StandardApplicantIngressApplyService applyService;

    public StandardApplicantDataIngressProcessor(
            CsdsIngressProperties properties,
            CsdsAuditService csdsAuditService,
            CsdsIngressTransactionRunner csdsIngressTransactionRunner,
            StandardApplicantDiffService diffService,
            StandardApplicantDiffReportingService diffReportingService,
            StandardApplicantIngressApplyService applyService) {
        super(
                properties,
                properties.getProcessors().getStandardApplicants(),
                csdsAuditService,
                csdsIngressTransactionRunner);
        standardApplicantProperties = properties.getProcessors().getStandardApplicants();
        this.diffService = diffService;
        this.diffReportingService = diffReportingService;
        this.applyService = applyService;
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
                        .map(this::withResolvedSaId)
                        .toList();
        ObjectNode normalisedPage = rawJson.getFirst().deepCopy();
        val recordsArray = normalisedPage.putArray("records");
        resolvedRecords.forEach(recordsArray::add);
        return List.of(normalisedPage);
    }

    @Override
    protected String queryParameters() {
        return standardApplicantProperties.getParameters();
    }

    @Override
    protected String queryPathType() {
        return "named-query";
    }

    @Override
    protected boolean usesCountEndpoint() {
        return false;
    }

    @Override
    protected String mockFilePath() {
        return standardApplicantProperties.getMock();
    }

    @Override
    protected StandardApplicantDiffResult diff(List<JsonNode> processedData) {
        return diffService.diff(
                new StandardApplicantDiffRequest(
                        targetTable(), processedData, this::toSourceRecord, this::extractRecords));
    }

    @Override
    protected void logDiffSummary(StandardApplicantDiffResult diff) {
        log.info(
                "CSDS ingress processor {} produced inserts={}, updates={}",
                datasetName(),
                countByOperation(diff, IngressOperation.INSERT),
                countByOperation(diff, IngressOperation.UPDATE));
    }

    @Override
    protected void report(List<JsonNode> processedData, StandardApplicantDiffResult diff) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                diff,
                this::extractRecords);
    }

    @Override
    protected void applyDiff(StandardApplicantDiffResult diff) {
        applyService.reconcileAndUpsert(targetTable(), targetKeyField(), diff);
    }

    @Override
    protected List<CsdsAuditEntry> buildSuccessAudits(
            List<JsonNode> processedData, StandardApplicantDiffResult diff) {
        return buildSuccessAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                StandardApplicantIngressRecord::id);
    }

    @Override
    protected List<CsdsAuditEntry> buildFailureAudits(
            List<JsonNode> processedData,
            StandardApplicantDiffResult diff,
            CsdsBatchUpsertException ex) {
        return buildFailureAuditEntries(
                diff.diffRecords(),
                sourceRecordsById(processedData),
                StandardApplicantIngressRecord::id,
                StandardApplicantIngressRecord.class,
                ex);
    }

    @Override
    public String processorName() {
        return CsdsIngestProcessorName.STANDARD_APPLICANTS.getExternalName();
    }

    @Override
    public CsdsIngestResponse ingest(List<JsonNode> rawJson) {
        val processedData = preProcess(rawJson);
        val diff = applyWithAuditing(processedData);
        return new CsdsIngestResponse()
                .inserted(countByOperation(diff, IngressOperation.INSERT))
                .updated(countByOperation(diff, IngressOperation.UPDATE));
    }

    private int countByOperation(StandardApplicantDiffResult diff, IngressOperation operation) {
        return Math.toIntExact(
                diff.diffRecords().stream().filter(item -> item.operation() == operation).count());
    }

    private StandardApplicantIngressRecord toSourceRecord(JsonNode node) {
        val address = firstAddress(node);
        return new StandardApplicantIngressRecord(
                requiredLong(node, SA_ID),
                requiredText(node, "Code"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "Enddate"),
                requiredLong(node, "RevisionNumber"),
                nullableText(node, "OrganisationName"),
                addressLine1(address),
                nestedText(address, "AddressLine2"),
                nestedText(address, "AddressLine3"),
                nestedText(address, "AddressLine4"),
                nestedText(address, "AddressLine5"),
                nestedText(address, "PostCode"),
                contactValue(node, "Email Address"),
                contactValue(node, "Telephone"));
    }

    private JsonNode withResolvedSaId(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return node;
        }
        val copiedRecord = objectNode.deepCopy();
        val psssaId = nullableLong(copiedRecord, "PSSSAID");
        val applicantId = nullableLong(copiedRecord, "ApplicantID");
        if (psssaId != null) {
            copiedRecord.put(SA_ID, psssaId);
        } else if (applicantId != null) {
            copiedRecord.put(SA_ID, applicantId + 100000L);
        }
        return copiedRecord;
    }

    private JsonNode firstAddress(JsonNode node) {
        val addresses = node.get("Address");
        return addresses != null && addresses.isArray() && !addresses.isEmpty()
                ? addresses.get(0)
                : null;
    }

    private String addressLine1(JsonNode address) {
        val line = nestedText(address, "AddressLine1");
        // CSDS can omit the address block; preserve the record while making the gap explicit.
        return StringUtils.hasText(line) ? line : MISSING_ADDRESS;
    }

    private String contactValue(JsonNode node, String contactType) {
        val contacts = node.get("ContactInformation");
        if (contacts == null || !contacts.isArray()) {
            return null;
        }
        for (val contact : contacts) {
            if (contactType.equals(nullableText(contact, "ContactType"))) {
                return nullableText(contact, "ContactValue");
            }
        }
        return null;
    }

    private String nestedText(JsonNode node, String fieldName) {
        return node == null ? null : nullableText(node, fieldName);
    }

    private Map<Long, JsonNode> sourceRecordsById(List<JsonNode> processedData) {
        return indexSourceRecords(processedData, node -> nullableLong(node, SA_ID));
    }
}
