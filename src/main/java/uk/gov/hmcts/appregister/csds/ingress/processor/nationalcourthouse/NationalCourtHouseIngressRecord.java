package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import lombok.With;
import org.jspecify.annotations.Nullable;

public record NationalCourtHouseIngressRecord(
        @With Long id,
        Long csdsId,
        String name,
        Long version,
        LocalDate startDate,
        LocalDate endDate,
        String courtLocationCode,
        String welshName) {
    private static final String COURT_ID_FIELD = "CourtID";
    private static final String PSS_NATIONAL_COURTHOUSE_ID_FIELD = "PSSNationalCourthouseID";
    private static final long NEW_RECORD_ID_OFFSET = 100000L;

    public static @Nullable Long calculateId(
            @Nullable Long pssNationalCourtHouseId, @Nullable Long courtId) {
        if (pssNationalCourtHouseId != null) {
            return pssNationalCourtHouseId;
        }
        return courtId == null ? null : Math.addExact(courtId, NEW_RECORD_ID_OFFSET);
    }

    public static @Nullable Long resolveId(JsonNode node) {
        return calculateId(
                nullableLong(node, PSS_NATIONAL_COURTHOUSE_ID_FIELD),
                nullableLong(node, COURT_ID_FIELD));
    }

    private static @Nullable Long nullableLong(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        return field == null || !field.canConvertToLong() ? null : field.longValue();
    }
}
