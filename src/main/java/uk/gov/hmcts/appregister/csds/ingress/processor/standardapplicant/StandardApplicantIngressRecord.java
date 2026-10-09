package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

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
        String telephoneNumber,
        Long pssId) {

    // Database rows have only the resolved ID; incoming rows retain both source IDs until matching.
    public StandardApplicantIngressRecord(
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
            String telephoneNumber) {
        this(
                id,
                code,
                startDate,
                endDate,
                version,
                name,
                addressLine1,
                addressLine2,
                addressLine3,
                addressLine4,
                addressLine5,
                postcode,
                emailAddress,
                telephoneNumber,
                null);
    }

    public StandardApplicantIngressRecord withId(Long resolvedId) {
        return new StandardApplicantIngressRecord(
                resolvedId,
                code,
                startDate,
                endDate,
                version,
                name,
                addressLine1,
                addressLine2,
                addressLine3,
                addressLine4,
                addressLine5,
                postcode,
                emailAddress,
                telephoneNumber,
                pssId);
    }

    public static @Nullable Long calculateId(
            @Nullable Long pssApplicantId, @Nullable Long applicantId) {
        if (pssApplicantId != null) {
            return pssApplicantId;
        }
        return applicantId == null ? null : Math.addExact(applicantId, 100000L);
    }
}
