package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

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
        Long version,
        Long pssId) {
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    public FeeIngressRecord(
            Long id,
            String reference,
            String description,
            BigDecimal amount,
            LocalDate startDate,
            LocalDate endDate,
            Long version) {
        this(id, reference, description, amount, startDate, endDate, version, null);
    }

    public record BusinessKey(String reference, LocalDate startDate) {}

    public BusinessKey businessKey() {
        return new BusinessKey(reference, startDate);
    }

    public FeeIngressRecord withId(Long resolvedId) {
        return new FeeIngressRecord(
                resolvedId, reference, description, amount, startDate, endDate, version, pssId);
    }

    public static @Nullable Long calculateId(
            @Nullable Long pssFixedListId, @Nullable Long civilFeeId) {
        if (pssFixedListId != null) {
            return pssFixedListId;
        }

        return civilFeeId == null ? null : Math.addExact(civilFeeId, NEW_RECORD_ID_OFFSET);
    }
}
