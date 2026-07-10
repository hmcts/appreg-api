package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record FeeIngressRecord(
        Long id,
        String reference,
        String description,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        Long version) {
    private static final String FEE_ID_FIELD = "FEE_ID";
    private static final String CIVIL_FEE_ID_FIELD = "CivilFeeID";
    private static final String PSS_FIXED_LIST_ID_FIELD = "PSSFixedListID";
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    public static @Nullable Long calculateId(
            @Nullable Long pssFixedListId, @Nullable Long civilFeeId) {
        if (pssFixedListId != null) {
            return pssFixedListId;
        }

        return civilFeeId == null ? null : civilFeeId + NEW_RECORD_ID_OFFSET;
    }

    public static @Nullable Long resolveId(JsonNode node) {
        var resolvedId = nullableLong(node, FEE_ID_FIELD);
        if (resolvedId != null) {
            return resolvedId;
        }

        return calculateId(
                nullableLong(node, PSS_FIXED_LIST_ID_FIELD),
                nullableLong(node, CIVIL_FEE_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return (field == null || !field.canConvertToLong()) ? null : field.longValue();
    }
}
