package uk.gov.hmcts.appregister.dto.write;

import java.time.LocalDate;

public record ApplicationListWriteDto(
        String status, LocalDate date, String time, String description, Long courthouseId) {}
