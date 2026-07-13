package uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

public record ApplicationCodeIngressRecord(
        Long id,
        String code,
        String title,
        String wording,
        String legislation,
        YesOrNo feeDue,
        YesOrNo requiresRespondent,
        LocalDate startDate,
        LocalDate endDate,
        YesOrNo bulkRespondentAllowed,
        Long version,
        String feeReference) {
    private static final String AC_ID_FIELD = "AC_ID";
    private static final String APPLICATION_CODE_ID_FIELD = "ApplicationCodeID";
    private static final String PSS_APPLICATION_CODE_ID_FIELD = "PSSApplicationCodeID";
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    static ApplicationCodeIngressRecord fromEntity(ApplicationCode applicationCode) {
        return new ApplicationCodeIngressRecord(
                applicationCode.getId(),
                applicationCode.getCode(),
                applicationCode.getTitle(),
                applicationCode.getWording(),
                applicationCode.getLegislation(),
                applicationCode.getFeeDue(),
                applicationCode.getRequiresRespondent(),
                applicationCode.getStartDate(),
                applicationCode.getEndDate(),
                applicationCode.getBulkRespondentAllowed(),
                applicationCode.getVersion(),
                applicationCode.getFeeReference());
    }

    public static @Nullable Long calculateId(
            @Nullable Long pssacid, @Nullable Long applicationCodeId) {
        if (pssacid != null) {
            return pssacid;
        }

        return applicationCodeId == null ? null : applicationCodeId + NEW_RECORD_ID_OFFSET;
    }

    public static @Nullable Long resolveId(JsonNode node) {
        var resolvedId = nullableLong(node, AC_ID_FIELD);
        if (resolvedId != null) {
            return resolvedId;
        }

        return calculateId(
                nullableLong(node, PSS_APPLICATION_CODE_ID_FIELD),
                nullableLong(node, APPLICATION_CODE_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return (field == null || !field.canConvertToLong()) ? null : field.longValue();
    }
}
