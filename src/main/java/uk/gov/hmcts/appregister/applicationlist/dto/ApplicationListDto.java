package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;

public record ApplicationListDto(
        Long id,
        String status,
        LocalDate date,
        String time,
        String description,
        NationalCourtHouseDto courthouse,
        String changedBy,
        LocalDate changedDate,
        Integer version) {}
