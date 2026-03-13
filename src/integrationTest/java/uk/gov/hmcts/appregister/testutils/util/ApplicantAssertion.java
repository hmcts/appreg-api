package uk.gov.hmcts.appregister.testutils.util;

import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;

/**
 * A utility that allows us to compare between a payload and entity.
 */
public class ApplicantAssertion {
    public static void validatePerson(Person applicant, StandardApplicant standardApplicant) {
        Assertions.assertNull(standardApplicant.getName());

        // assert applicant
        Assertions.assertEquals(
                applicant.getName().getSurname(), standardApplicant.getApplicantSurname());
        Assertions.assertEquals(
                applicant.getName().getThirdForename().get(),
                standardApplicant.getApplicantForename3());
        Assertions.assertEquals(
                applicant.getName().getSecondForename().get(),
                standardApplicant.getApplicantForename2());
        Assertions.assertEquals(
                applicant.getName().getFirstForename(), standardApplicant.getApplicantForename1());
        Assertions.assertEquals(
                applicant.getContactDetails().getPostcode(), standardApplicant.getPostcode());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine1(),
                standardApplicant.getAddressLine1());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine2().orElse(null),
                standardApplicant.getAddressLine2());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine3().orElse(null),
                standardApplicant.getAddressLine3());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine4().orElse(null),
                standardApplicant.getAddressLine4());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine5().orElse(null),
                standardApplicant.getAddressLine5());
        Assertions.assertEquals(
                applicant.getContactDetails().getPhone().orElse(null),
                standardApplicant.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getMobile().orElse(null),
                standardApplicant.getMobileNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getEmail().orElse(null),
                standardApplicant.getEmailAddress());
    }

    public static void validatePerson(Person applicant, NameAddress applicationListEntry) {
        Assertions.assertNull(applicationListEntry.getName());

        // assert applicant
        Assertions.assertEquals(
                applicant.getName().getSurname(), applicationListEntry.getSurname());
        Assertions.assertEquals(
                applicant.getName().getThirdForename().get(), applicationListEntry.getForename3());
        Assertions.assertEquals(
                applicant.getName().getFirstForename(), applicationListEntry.getForename1());
        Assertions.assertEquals(
                applicant.getName().getSecondForename().get(), applicationListEntry.getForename2());
        Assertions.assertEquals(
                applicant.getContactDetails().getPostcode(), applicationListEntry.getPostcode());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine1(),
                applicationListEntry.getAddress1());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine2().orElse(null),
                applicationListEntry.getAddress2());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine3().orElse(null),
                applicationListEntry.getAddress3());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine4().orElse(null),
                applicationListEntry.getAddress4());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine5().orElse(null),
                applicationListEntry.getAddress5());
        Assertions.assertEquals(
                applicant.getContactDetails().getPhone().orElse(null),
                applicationListEntry.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getMobile().orElse(null),
                applicationListEntry.getMobileNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getEmail().orElse(null),
                applicationListEntry.getEmailAddress());
    }

    public static void validateOrganisation(
            Organisation applicant, StandardApplicant standardApplicant) {

        // assert applicant
        Assertions.assertEquals(applicant.getName(), standardApplicant.getName());

        Assertions.assertEquals(
                applicant.getContactDetails().getPostcode(), standardApplicant.getPostcode());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine1(),
                standardApplicant.getAddressLine1());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine2().orElse(null),
                standardApplicant.getAddressLine2());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine3().orElse(null),
                standardApplicant.getAddressLine3());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine4().orElse(null),
                standardApplicant.getAddressLine4());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine5().orElse(null),
                standardApplicant.getAddressLine5());
        Assertions.assertEquals(
                applicant.getContactDetails().getPhone().orElse(null),
                standardApplicant.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getMobile().orElse(null),
                standardApplicant.getMobileNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getEmail().orElse(null),
                standardApplicant.getEmailAddress());
    }

    public static void validateOrganisation(
            Organisation applicant, NameAddress applicationListEntry) {

        // assert applicant
        Assertions.assertEquals(applicant.getName(), applicationListEntry.getName());

        Assertions.assertEquals(
                applicant.getContactDetails().getPostcode(), applicationListEntry.getPostcode());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine1(),
                applicationListEntry.getAddress1());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine2().orElse(null),
                applicationListEntry.getAddress2());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine3().orElse(null),
                applicationListEntry.getAddress3());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine4().orElse(null),
                applicationListEntry.getAddress4());
        Assertions.assertEquals(
                applicant.getContactDetails().getAddressLine5().orElse(null),
                applicationListEntry.getAddress5());
        Assertions.assertEquals(
                applicant.getContactDetails().getPhone().orElse(null),
                applicationListEntry.getTelephoneNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getMobile().orElse(null),
                applicationListEntry.getMobileNumber());
        Assertions.assertEquals(
                applicant.getContactDetails().getEmail().orElse(null),
                applicationListEntry.getEmailAddress());
    }
}
