package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.courtlocation.dto.CourtHouseDto;

public record ApplicationListDto(
        Long id,
        String status,
        LocalDate date,
        String time,
        String description,
        CourtHouseDto courthouse,
        String changedBy,
        LocalDate changedDate,
        Integer version) {}
