package uk.gov.hmcts.appregister.applicationlist.dto;

import java.time.LocalDate;

public record ApplicationListWriteDto(
        String status, LocalDate date, String time, String description, Long courthouseId) {}
