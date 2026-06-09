package uk.gov.hmcts.appregister.csds.ingress;

import lombok.val;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(
        prefix = "appreg.csds.ingress.processors.application-codes",
        name = "enabled",
        havingValue = "true")
class ApplicationCodeDataIngressProcessor
        extends AbstractPagedCsdsIngressProcessor<List<JsonNode>> {
    private static final String TARGET_TABLE = "application_codes";
    private static final String TARGET_KEY_FIELD = "ac_id";

    private final ApplicationCodeDiffReportingService diffReportingService;

    ApplicationCodeDataIngressProcessor(
            CsdsIngressProperties properties,
            ApplicationCodeDiffReportingService diffReportingService) {
        super(properties, properties.getProcessors().getApplicationCodes().getSourceEntityName());
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
    public void handle(List<JsonNode> processedData) {
        diffReportingService.reportDiff(
                datasetName(),
                targetTable(),
                targetKeyField(),
                processedData,
                this::toSourceRecord,
                this::extractRecords);
    }

    private ApplicationCodeIngressRecord toSourceRecord(JsonNode record) {
        return new ApplicationCodeIngressRecord(
                requiredLong(record, "ApplicationCodeID"),
                requiredText(record, "Code"),
                requiredText(record, "ApplicationTitle"),
                requiredText(record, "ApplicationWording"),
                nullableText(record, "Legislation"),
                requiredYesOrNo(record, "FeeDue"),
                requiredYesOrNo(record, "Respondent"),
                requiredLocalDate(record, "StartDate"),
                nullableLocalDate(record, "EndDate"),
                requiredYesOrNo(record, "BulkRespondentAllowed"),
                requiredLong(record, "VersionNumber"),
                nullableText(record, "FeeReference"));
    }

    private Long requiredLong(JsonNode record, String fieldName) {
        val field = record.get(fieldName);
        if (field == null || !field.canConvertToLong()) {
            throw invalidField(fieldName);
        }

        return field.longValue();
    }

    private String requiredText(JsonNode record, String fieldName) {
        val value = nullableText(record, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        return value;
    }

    private String nullableText(JsonNode record, String fieldName) {
        val field = record.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }

        return field.asText();
    }

    private YesOrNo requiredYesOrNo(JsonNode record, String fieldName) {
        val value = nullableText(record, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        try {
            return YesOrNo.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field "
                            + fieldName
                            + " contained an unknown YesOrNo value for "
                            + datasetName(),
                    ex);
        }
    }

    private LocalDate requiredLocalDate(JsonNode record, String fieldName) {
        val value = nullableLocalDate(record, fieldName);
        if (value == null) {
            throw invalidField(fieldName);
        }

        return value;
    }

    private LocalDate nullableLocalDate(JsonNode record, String fieldName) {
        val value = nullableText(record, fieldName);
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR,
                    "CSDS field " + fieldName + " contained an invalid date for " + datasetName(),
                    ex);
        }
    }

    private AppRegistryException invalidField(String fieldName) {
        return new AppRegistryException(
                CommonAppError.INTERNAL_SERVER_ERROR,
                "CSDS field " + fieldName + " was missing or invalid for " + datasetName());
    }
}
