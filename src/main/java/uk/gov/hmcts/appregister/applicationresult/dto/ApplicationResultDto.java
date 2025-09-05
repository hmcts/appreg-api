package uk.gov.hmcts.appregister.applicationresult.dto;

import java.time.LocalDate;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;

/** Application Result Data Transfer Object. */
public record ApplicationResultDto(
    Long id,
    ResolutionCodeDto resultCode,
    String resultWording,
    String resultOfficer,
    String changedBy,
    LocalDate changedDate,
    Integer version) {}
