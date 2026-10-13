package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

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
        Long version,
        Long pssId) {
    // Database rows have only the resolved ID; incoming rows retain both source IDs until matching.
    public ResolutionCodeIngressRecord(
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
        this(
                id,
                code,
                title,
                wording,
                legislation,
                recipient1Email,
                recipient2Email,
                startDate,
                endDate,
                version,
                null);
    }

    public ResolutionCodeIngressRecord withId(Long resolvedId) {
        return new ResolutionCodeIngressRecord(
                resolvedId,
                code,
                title,
                wording,
                legislation,
                recipient1Email,
                recipient2Email,
                startDate,
                endDate,
                version,
                pssId);
    }

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
}
