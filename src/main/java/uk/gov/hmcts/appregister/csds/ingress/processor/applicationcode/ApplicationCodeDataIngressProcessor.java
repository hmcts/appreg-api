package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProperties;
import uk.gov.hmcts.appregister.csds.ingress.processor.AbstractPagedCsdsIngressProcessor;

@Component
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.processors.application-codes",
        name = "enabled",
        havingValue = "true")
public class ApplicationCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>, ApplicationCodeDiffResult> {
    private static final String TARGET_TABLE = "application_codes";
    private static final String TARGET_KEY_FIELD = "ac_id";

    private final CsdsIngressProperties.ApplicationCodes applicationCodeProperties;
    private final ApplicationCodeDiffService diffService;
    private final ApplicationCodeDiffReportingService diffReportingService;

    public ApplicationCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            ApplicationCodeDiffService diffService,
            ApplicationCodeDiffReportingService diffReportingService) {
        super(properties, properties.getProcessors().getApplicationCodes().getSourceEntityName());
        this.applicationCodeProperties = properties.getProcessors().getApplicationCodes();
        this.diffService = diffService;
        this.diffReportingService = diffReportingService;
    }

    @Override
    public String targetTable() {
        return TARGET_TABLE;
    }

    @Override
    public String targetKeyField() {
        return TARGET_KEY_FIELD;
    }

    @Override
    public List<JsonNode> preProcess(List<JsonNode> rawJson) {
        return rawJson;
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
        return diffService.diff(processedData, this::toSourceRecord, this::extractRecords);
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

    private ApplicationCodeIngressRecord toSourceRecord(JsonNode node) {
        return new ApplicationCodeIngressRecord(
                requiredResolvedApplicationCodeKey(node),
                requiredText(node, "Code"),
                requiredText(node, "ApplicationTitle"),
                requiredText(node, "ApplicationWording"),
                nullableText(node, "Legislation"),
                requiredYesOrNo(node, "FeeDue"),
                requiredYesOrNo(node, "Respondent"),
                requiredLocalDate(node, "StartDate"),
                nullableLocalDate(node, "EndDate"),
                requiredYesOrNo(node, "BulkRespondentAllowed"),
                requiredLong(node, "VersionNumber"),
                nullableText(node, "FeeReference"));
    }

    private Long requiredResolvedApplicationCodeKey(JsonNode node) {
        val value = ApplicationCodeIngressRecord.resolveId(node);
        if (value == null) {
            throw invalidField("PSSACID/ApplicationCodeID");
        }

        return value;
    }
}
