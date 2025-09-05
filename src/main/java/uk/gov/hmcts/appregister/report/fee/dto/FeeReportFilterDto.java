package uk.gov.hmcts.appregister.report.fee.dto;

import java.time.LocalDate;

/** DTO for filtering fee reports. */
public record FeeReportFilterDto(
    LocalDate startDate,
    LocalDate endDate,
    String standardApplicantCode,
    String applicantSurname,
    String courthouseCode) {}
