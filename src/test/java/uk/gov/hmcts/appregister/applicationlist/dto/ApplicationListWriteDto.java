package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.OffsetDateTime;

public record ApplicationListWriteDto(
        String status,
        OffsetDateTime date,
        OffsetDateTime time,
        String description,
        Long courthouseId) {}
