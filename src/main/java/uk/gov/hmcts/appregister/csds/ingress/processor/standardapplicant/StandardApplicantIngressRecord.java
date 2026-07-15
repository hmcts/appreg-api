package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.time.LocalDate;

public record StandardApplicantIngressRecord(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        Long version,
        String name,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String addressLine4,
        String addressLine5,
        String postcode,
        String emailAddress,
        String telephoneNumber) {}
