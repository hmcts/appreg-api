package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import lombok.With;
import org.jspecify.annotations.Nullable;

public record ResolutionCodeIngressRecord(
        @With Long id,
        String code,
        String title,
        String wording,
        String legislation,
        String recipient1Email,
        String recipient2Email,
        LocalDate startDate,
        LocalDate endDate,
        Long version,
        Long csdsId) {
    private static final String RESOLUTION_CODE_ID_FIELD = "ResolutionCodeID";
    private static final String PSS_RESOLUTION_CODE_ID_FIELD = "PSSResolutionCodeID";
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    public static @Nullable Long calculateId(
            @Nullable Long pssResolutionCodeId, @Nullable Long resolutionCodeId) {
        if (pssResolutionCodeId != null) {
            return pssResolutionCodeId;
        }

        return resolutionCodeId == null
                ? null
                : Math.addExact(resolutionCodeId, NEW_RECORD_ID_OFFSET);
    }

    public static @Nullable Long resolveId(JsonNode node) {
        return calculateId(
                nullableLong(node, PSS_RESOLUTION_CODE_ID_FIELD),
                nullableLong(node, RESOLUTION_CODE_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return (field == null || !field.canConvertToLong()) ? null : field.longValue();
    }
}
