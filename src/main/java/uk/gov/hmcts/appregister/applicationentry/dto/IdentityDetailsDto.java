package uk.gov.hmcts.appregister.applicationentry.dto;

import java.time.OffsetDateTime;

/** DTO representing identity details. */
public record IdentityDetailsDto(
    Long id,
    String code,
    String name,
    String title,
    String forename1,
    String forename2,
    String forename3,
    String surname,
    String addressLine1,
    String addressLine2,
    String addressLine3,
    String addressLine4,
    String addressLine5,
    String postcode,
    String emailAddress,
    String telephoneNumber,
    String mobileNumber,
    OffsetDateTime dateOfBirth) {}
