package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record ResolutionCodeIngressRecord(
        Long id,
        String code,
        String title,
        String wording,
        String legislation,
        String recipient1Email,
        String recipient2Email,
        LocalDate startDate,
        LocalDate endDate,
        Long version) {
    private static final String RC_ID_FIELD = "RC_ID";
    private static final String RESOLUTION_CODE_ID_FIELD = "ResolutionCodeID";
    private static final String PSS_RESOLUTION_CODE_ID_FIELD = "PSSRCID";
    private static final long RESOLUTION_CODE_ID_OFFSET = 100000L;

    public static @Nullable Long calculateId(
            @Nullable Long pssrcid, @Nullable Long resolutionCodeId) {
        if (pssrcid != null) {
            return pssrcid;
        }

        return resolutionCodeId == null ? null : resolutionCodeId + RESOLUTION_CODE_ID_OFFSET;
    }

    public static @Nullable Long resolveId(JsonNode node) {
        var resolvedId = nullableLong(node, RC_ID_FIELD);
        if (resolvedId != null) {
            return resolvedId;
        }

        return calculateId(
                nullableLong(node, PSS_RESOLUTION_CODE_ID_FIELD),
                nullableLong(node, RESOLUTION_CODE_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return (field == null || !field.canConvertToLong()) ? null : field.longValue();
    }
}
