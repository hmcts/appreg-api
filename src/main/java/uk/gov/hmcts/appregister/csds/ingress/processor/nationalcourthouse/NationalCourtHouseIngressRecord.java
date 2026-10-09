package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record NationalCourtHouseIngressRecord(
        Long id,
        String name,
        Long version,
        LocalDate startDate,
        LocalDate endDate,
        String courtLocationCode,
        String welshName,
        Long pssId) {
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    // Database rows contain resolved IDs; incoming rows retain both source IDs until name matching.
    public NationalCourtHouseIngressRecord(
            Long id,
            String name,
            Long version,
            LocalDate startDate,
            LocalDate endDate,
            String courtLocationCode,
            String welshName) {
        this(id, name, version, startDate, endDate, courtLocationCode, welshName, null);
    }

    public NationalCourtHouseIngressRecord withId(Long resolvedId) {
        return new NationalCourtHouseIngressRecord(
                resolvedId, name, version, startDate, endDate, courtLocationCode, welshName, pssId);
    }

    public static @Nullable Long calculateId(
            @Nullable Long pssNationalCourtHouseId, @Nullable Long courtId) {
        if (pssNationalCourtHouseId != null) {
            return pssNationalCourtHouseId;
        }
        return courtId == null ? null : Math.addExact(courtId, NEW_RECORD_ID_OFFSET);
    }
}
