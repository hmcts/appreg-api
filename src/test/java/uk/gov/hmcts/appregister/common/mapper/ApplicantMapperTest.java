package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Person;

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
        var lastNameOnly = new NameAddress();
        lastNameOnly.setLastName("Doe");

        var firstNameOnly = new NameAddress();
        firstNameOnly.setFirstName("Jane");

        var noPersonName = new NameAddress();

        assertEquals("Doe", mapper.getNameForNameAddress(lastNameOnly));
        assertEquals("Jane", mapper.getNameForNameAddress(firstNameOnly));
        assertEquals("", mapper.getNameForNameAddress(noPersonName));
    }

    @Test
    void getNameForApplicant_fallsBackToNameAddressWhenStandardApplicantMissing() {
        var applicant = new NameAddress();
        applicant.setFirstName("Sarah");
        applicant.setLastName("Johnson");

        assertEquals("Sarah Johnson", mapper.getNameForApplicant(null, applicant));
    }

    @Test
    void toApplicantNameAddress_mapsGdsNameFieldsToCanonicalAndLegacyColumns() {
        final var person = new Person();
        var name = new FullName();
        name.setFirstName("Ada");
        name.setMiddleName(JsonNullable.of("Byron"));
        name.setLastName("Lovelace");
        person.setName(name);
        person.setContactDetails(new ContactDetails().addressLine1("1 High Street"));

        var mapped = mapper.toApplicantNameAddress(new Applicant().person(person));

        assertEquals("Ada", mapped.getFirstName());
        assertEquals("Byron", mapped.getMiddleName());
        assertEquals("Lovelace", mapped.getLastName());
        assertEquals("Ada", mapped.getForename1());
        assertEquals("Byron", mapped.getForename2());
        assertNull(mapped.getForename3());
        assertEquals("Lovelace", mapped.getSurname());
    }

    @Test
    void toFullName_populatesGdsFieldsFromCanonicalColumns() {
        var entity = new NameAddress();
        entity.setTitle("Ms");
        entity.setFirstName("Ada");
        entity.setMiddleName("Byron");
        entity.setLastName("Lovelace");

        var fullName = mapper.toFullName(entity);

        assertEquals("Ada", fullName.getFirstName());
        assertEquals(JsonNullable.of("Byron"), fullName.getMiddleName());
        assertEquals("Lovelace", fullName.getLastName());
    }
}
