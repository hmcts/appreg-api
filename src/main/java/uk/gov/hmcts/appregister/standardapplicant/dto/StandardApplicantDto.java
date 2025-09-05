package uk.gov.hmcts.appregister.standardapplicant.dto;

import java.time.LocalDate;

/** DTO for Standard Applicant. */
public record StandardApplicantDto(
        Long id,
        String applicantCode,
        String applicantTitle,
        String applicantName,
        String applicantForename1,
        String applicantForename2,
        String applicantForename3,
        String applicantSurname,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String addressLine4,
        String addressLine5,
        String postcode,
        String emailAddress,
        String telephoneNumber,
        String mobileNumber,
        LocalDate applicantStartDate,
        LocalDate applicantEndDate) {}
