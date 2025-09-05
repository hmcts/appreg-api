package uk.gov.hmcts.appregister.report.fee.dto;

import java.time.LocalDate;

/** DTO representing a row in the fee report. */
public record FeeReportRowDto(
    LocalDate listDate,
    String courthouseName,
    String otherCourthouse,
    String standardApplicantCode,
    String applicantNameOrSurname,
    String applicationCode,
    String applicationCodeTitle) {}
