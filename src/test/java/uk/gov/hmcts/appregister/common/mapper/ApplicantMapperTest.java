package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;

class ApplicantMapperTest {

    private final ApplicantMapper mapper = new ApplicantMapperImpl();

    @Test
    void getNameForApplicant_returnsOrganisationNameWhenPresent() {
        var standardApplicant = new StandardApplicant();
        standardApplicant.setName("Applicant Org");
        standardApplicant.setApplicantForename1("Ignored");
        standardApplicant.setApplicantSurname("Ignored");

        assertEquals("Applicant Org", mapper.getNameForApplicant(standardApplicant, null));
    }

    @Test
    void getNameForApplicant_returnsFormattedPersonNameForIndividuals() {
        var standardApplicant = new StandardApplicant();
        standardApplicant.setApplicantForename1("Jane");
        standardApplicant.setApplicantSurname("Doe");

        assertEquals("Jane Doe", mapper.getNameForApplicant(standardApplicant, null));
    }

    @Test
    void getNameForNameAddress_formatsPartialAndMissingPersonNames() {
        var surnameOnly = new NameAddress();
        surnameOnly.setSurname("Doe");

        var forenameOnly = new NameAddress();
        forenameOnly.setForename1("Jane");

        var noPersonName = new NameAddress();

        assertEquals("Doe", mapper.getNameForNameAddress(surnameOnly));
        assertEquals("Jane", mapper.getNameForNameAddress(forenameOnly));
        assertEquals("", mapper.getNameForNameAddress(noPersonName));
    }

    @Test
    void getNameForApplicant_fallsBackToNameAddressWhenStandardApplicantMissing() {
        var applicant = new NameAddress();
        applicant.setForename1("Sarah");
        applicant.setSurname("Johnson");

        assertEquals("Sarah Johnson", mapper.getNameForApplicant(null, applicant));
    }
}
