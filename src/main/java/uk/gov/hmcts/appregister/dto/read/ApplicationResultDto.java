package uk.gov.hmcts.appregister.dto.read;

import java.time.LocalDate;

public record ApplicationResultDto(
        Long id,
        ResultCodeDto resultCode,
        String resultWording,
        String resultOfficer,
        String changedBy,
        LocalDate changedDate,
        Integer version) {}
