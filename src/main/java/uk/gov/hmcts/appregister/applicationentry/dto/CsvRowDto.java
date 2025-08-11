package uk.gov.hmcts.appregister.applicationentry.dto;

public record CsvRowDto(
        String standardApplicantCode,
        String respondentTitle,
        String respondentOrganisationName,
        String respondentForename1,
        String respondentForename2,
        String respondentForename3,
        String respondentSurname,
        String respondentAddressLine1,
        String respondentAddressLine2,
        String respondentAddressLine3,
        String respondentAddressLine4,
        String respondentAddressLine5,
        String respondentPostcode,
        String respondentEmail,
        String respondentTelephone,
        String respondentMobile,
        String accountNumber,
        String applicationCode,
        String applicationText1,
        String applicationText2) {}
