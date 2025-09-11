package uk.gov.hmcts.appregister.applicationlistentryresolutions.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;

/** Application Result Data Transfer Object. */
public record ApplicationListEntryResolutionDto(
        Long id,
        ResolutionCodeDto resultCode,
        String resultWording,
        String resultOfficer,
        String changedBy,
        OffsetDateTime changedDate,
        Long version) {}
