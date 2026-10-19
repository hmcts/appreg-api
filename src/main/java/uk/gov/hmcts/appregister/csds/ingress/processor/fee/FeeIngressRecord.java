package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import lombok.With;
import org.jspecify.annotations.Nullable;

public record FeeIngressRecord(
        @With Long id,
        Long csdsId,
        String reference,
        String description,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        Long version) {
    private static final String CIVIL_FEE_ID_FIELD = "CivilFeeID";
    private static final String PSS_FIXED_LIST_ID_FIELD = "PSSFixedListID";
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    public FeeIngressRecord(
            Long id,
            String reference,
            String description,
            BigDecimal amount,
            LocalDate startDate,
            LocalDate endDate,
            Long version) {
        this(id, null, reference, description, amount, startDate, endDate, version);
    }

    public record BusinessKey(String reference, LocalDate startDate) {}

    public BusinessKey businessKey() {
        return new BusinessKey(
                reference == null ? null : reference.toUpperCase(Locale.ROOT), startDate);
    }

    public static @Nullable Long calculateId(
            @Nullable Long pssFixedListId, @Nullable Long civilFeeId) {
        if (pssFixedListId != null) {
            return pssFixedListId;
        }

        return civilFeeId == null ? null : Math.addExact(civilFeeId, NEW_RECORD_ID_OFFSET);
    }

    public static @Nullable Long resolveId(JsonNode node) {
        return calculateId(
                nullableLong(node, PSS_FIXED_LIST_ID_FIELD),
                nullableLong(node, CIVIL_FEE_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return (field == null || !field.canConvertToLong()) ? null : field.longValue();
    }
}
