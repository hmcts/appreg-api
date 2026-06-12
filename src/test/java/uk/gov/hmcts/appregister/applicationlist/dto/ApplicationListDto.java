package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.generated.model.CourtLocationGetDetailDto;

/**
 * DTO for Application List entries.
 */
public record ApplicationListDto(
        Long id,
        String status,
        OffsetDateTime date,
        String time,
        String description,
        CourtLocationGetDetailDto courthouse,
        String changedBy,
        OffsetDateTime changedDate,
        Integer version) {}
