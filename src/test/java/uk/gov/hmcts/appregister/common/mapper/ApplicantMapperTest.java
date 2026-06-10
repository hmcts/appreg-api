package uk.gov.hmcts.appregister.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.RespondentPerson;

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
    void toApplicantNameAddress_mapsGdsNameFieldsToCanonicalColumns() {
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

    @Test
    void toApplicant_setsApplicantCodeAndMapsPersonDetails() {
        var fullName = new FullName();
        fullName.setTitle("Ms");
        fullName.setFirstName(" Ada ");
        fullName.setMiddleName(JsonNullable.of(" Byron "));
        fullName.setLastName(" Lovelace ");

        var contactDetails = new ContactDetails();
        contactDetails.setAddressLine1("1 High Street");
        contactDetails.setAddressLine2(JsonNullable.of("Line 2"));
        contactDetails.setAddressLine3(JsonNullable.of("Line 3"));
        contactDetails.setAddressLine4(JsonNullable.of("Line 4"));
        contactDetails.setAddressLine5(JsonNullable.of("Line 5"));
        contactDetails.setPostcode("AB1 2CD");
        contactDetails.setPhone(JsonNullable.of("01234"));
        contactDetails.setMobile(JsonNullable.of("07700"));
        contactDetails.setEmail(JsonNullable.of("ada@example.com"));

        var person = new Person();
        person.setName(fullName);
        person.setContactDetails(contactDetails);

        var applicant = new Applicant();
        applicant.setPerson(person);

        var mapped = mapper.toApplicant(applicant);

        assertEquals(NameAddressCodeType.APPLICANT, mapped.getCode());
        assertEquals("Ms", mapped.getTitle());
        assertEquals("Ada", mapped.getFirstName());
        assertEquals("Byron", mapped.getMiddleName());
        assertEquals("Lovelace", mapped.getLastName());
        assertEquals("1 High Street", mapped.getAddress1());
        assertEquals("Line 2", mapped.getAddress2());
        assertEquals("AB1 2CD", mapped.getPostcode());
        assertEquals("01234", mapped.getTelephoneNumber());
        assertEquals("07700", mapped.getMobileNumber());
        assertEquals("ada@example.com", mapped.getEmailAddress());
    }

    @Test
    void toRespondent_setsRespondentCodeAndPreservesDateOfBirth() {
        var fullName = new FullName();
        fullName.setFirstName("John");
        fullName.setMiddleName(JsonNullable.of("Quincy"));
        fullName.setLastName("Public");

        var contactDetails = new ContactDetails();
        contactDetails.setAddressLine1("2 Main Street");
        contactDetails.setPostcode("ZX1 9YZ");

        var person = new RespondentPerson();
        person.setName(fullName);
        person.setContactDetails(contactDetails);
        person.setDateOfBirth(LocalDate.of(1980, Month.JANUARY, 2));

        var respondent = new Respondent();
        respondent.setPerson(person);

        var mapped = mapper.toRespondent(respondent);

        assertEquals(NameAddressCodeType.RESPONDENT, mapped.getCode());
        assertEquals("John", mapped.getFirstName());
        assertEquals("Quincy", mapped.getMiddleName());
        assertEquals("Public", mapped.getLastName());
        assertEquals(LocalDate.of(1980, Month.JANUARY, 2), mapped.getDateOfBirth());
    }

    @Test
    void toApplicantNameAddress_returnsOrganisationAndNullForEmptyApplicant() {
        var organisation = new uk.gov.hmcts.appregister.generated.model.Organisation();
        organisation.setName("Org Ltd");
        organisation.setContactDetails(new ContactDetails().addressLine1("3 Org Street"));

        var applicant = new Applicant();
        applicant.setOrganisation(organisation);

        var mappedOrganisation = mapper.toApplicantNameAddress(applicant);

        assertEquals("Org Ltd", mappedOrganisation.getName());
        assertEquals("3 Org Street", mappedOrganisation.getAddress1());
        assertNull(mapper.toApplicantNameAddress(new Applicant()));
        assertNull(mapper.toApplicantNameAddress(null));
    }

    @Test
    void toApplicant_mapsOrganisationAndPersonEntitiesAndHandlesNull() {
        var organisationEntity = new NameAddress();
        organisationEntity.setName("Org Ltd");
        organisationEntity.setAddress1("3 Org Street");

        var organisation = mapper.toApplicant(organisationEntity);

        assertEquals("Org Ltd", organisation.getOrganisation().getName());
        assertEquals(
                "3 Org Street",
                organisation.getOrganisation().getContactDetails().getAddressLine1());

        var personEntity = new NameAddress();
        personEntity.setTitle("Ms");
        personEntity.setFirstName("Ada");
        personEntity.setMiddleName("Byron");
        personEntity.setLastName("Lovelace");

        var personApplicant = mapper.toApplicant(personEntity);

        assertEquals("Ada", personApplicant.getPerson().getName().getFirstName());
        assertEquals(
                JsonNullable.of("Byron"), personApplicant.getPerson().getName().getMiddleName());
        assertEquals("Lovelace", personApplicant.getPerson().getName().getLastName());

        assertNull(mapper.toApplicant((NameAddress) null));
    }

    @Test
    void toContactDetails_returnsEmptyDetailsForNullEntity() {
        var details = mapper.toContactDetails(null);

        assertNull(details.getAddressLine1());
        assertNull(details.getPostcode());
        assertEquals(JsonNullable.undefined(), details.getAddressLine2());
    }

    @Test
    void toContactDetails_mapsAllPresentFields() {
        var entity = new NameAddress();
        entity.setAddress1("1 High Street");
        entity.setAddress2("Line 2");
        entity.setAddress3("Line 3");
        entity.setAddress4("Line 4");
        entity.setAddress5("Line 5");
        entity.setPostcode("AB1 2CD");
        entity.setTelephoneNumber("01234");
        entity.setMobileNumber("07700");
        entity.setEmailAddress("ada@example.com");

        var details = mapper.toContactDetails(entity);

        assertEquals("1 High Street", details.getAddressLine1());
        assertEquals(JsonNullable.of("Line 2"), details.getAddressLine2());
        assertEquals(JsonNullable.of("Line 3"), details.getAddressLine3());
        assertEquals(JsonNullable.of("Line 4"), details.getAddressLine4());
        assertEquals(JsonNullable.of("Line 5"), details.getAddressLine5());
        assertEquals("AB1 2CD", details.getPostcode());
        assertEquals(JsonNullable.of("01234"), details.getPhone());
        assertEquals(JsonNullable.of("07700"), details.getMobile());
        assertEquals(JsonNullable.of("ada@example.com"), details.getEmail());
    }

    @Test
    void getNameForNameAddress_returnsOrganisationNameAndEmptyForNull() {
        var organisation = new NameAddress();
        organisation.setName("Org Ltd");

        assertEquals("Org Ltd", mapper.getNameForNameAddress(organisation));
        assertEquals("", mapper.getNameForNameAddress(null));
    }

    @Test
    void combineMiddleName_trimsAndSkipsBlankValues() {
        assertEquals("Anne Louise", ApplicantMapper.combineMiddleName(" Anne ", " Louise "));
        assertEquals("Anne", ApplicantMapper.combineMiddleName("Anne", " "));
        assertNull(ApplicantMapper.combineMiddleName(" ", null));
    }

    @Test
    void toRespondentNameAddress_returnsOrganisationAndNullForEmptyRespondent() {
        var organisation = new uk.gov.hmcts.appregister.generated.model.Organisation();
        organisation.setName("Widgets Ltd");
        organisation.setContactDetails(new ContactDetails().addressLine1("4 Widget Way"));

        var respondent = new Respondent();
        respondent.setOrganisation(organisation);

        var mappedOrganisation = mapper.toRespondentNameAddress(respondent);

        assertEquals("Widgets Ltd", mappedOrganisation.getName());
        assertEquals("4 Widget Way", mappedOrganisation.getAddress1());
        assertNull(mapper.toRespondentNameAddress(new Respondent()));
        assertNull(mapper.toRespondentNameAddress(null));
    }
}
