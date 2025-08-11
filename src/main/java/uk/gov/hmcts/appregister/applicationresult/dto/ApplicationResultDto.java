package uk.gov.hmcts.appregister.applicationresult.dto;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.resultcode.dto.ResultCodeDto;

public record ApplicationResultDto(
        Long id,
        ResultCodeDto resultCode,
        String resultWording,
        String resultOfficer,
        String changedBy,
        LocalDate changedDate,
        Integer version) {}
