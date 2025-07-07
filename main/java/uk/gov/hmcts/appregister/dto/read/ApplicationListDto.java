package uk.gov.hmcts.appregister.dto.read;

import java.time.LocalDate;

public record ApplicationListDto(
    Long id,
    String status,
    LocalDate date,
    String time,
    String description,
    CourtHouseDto courthouse,
    String changedBy,
    LocalDate changedDate,
    Integer version
) {}
