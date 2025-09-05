package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;

/** DTO for Application List entries. */
public record ApplicationListDto(
    Long id,
    String status,
    OffsetDateTime date,
    String time,
    String description,
    NationalCourtHouseDto courthouse,
    String changedBy,
    OffsetDateTime changedDate,
    Integer version) {}
