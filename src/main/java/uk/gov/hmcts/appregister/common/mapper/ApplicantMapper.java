package uk.gov.hmcts.appregister.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.Respondent;

/**
 * A useful mapper to convert to and from applicant and respondent dtos and the associated {@link
 * NameAddress} entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicantMapper {

    /**
     * Maps the applicant to a name address.
     *
     * @param applicant The applicant details
     * @return The mapped entity
     */
    public NameAddress toApplicant(Applicant applicant) {
        NameAddress nameAddress = toApplicantNameAddress(applicant);
        nameAddress.setCode(NameAddress.APPLICANT_CODE);
        return nameAddress;
    }

    /**
     * A useful mapper to map the applicant details of the standard applicant.
     *
     * @param applicant The database applicant name and address
     * @return The applicant Dto
     */
    public Applicant toApplicant(NameAddress applicant) {

        ContactDetails contactDetails = toContactDetails(applicant);
        Applicant applicantDto = null;
        if (applicant != null) {
            applicantDto = new Applicant();

            // if we dont have a name this is an organisation
            if (applicant.getName() != null) {
                // if the name is set then this is an organisation otherwise a person
                Organisation organisation = new Organisation();
                organisation.setName(applicant.getName());
                organisation.setContactDetails(contactDetails);
                applicantDto.setOrganisation(organisation);
            } else {
                Person person = new Person();
                FullName fullName = toFullName(applicant);
                person.setContactDetails(contactDetails);
                person.setName(fullName);
                applicantDto.setPerson(person);
            }
        }

        return applicantDto;
    }

    /**
     * to full name.
     *
     * @param applicant The standard applicant name and address
     * @return The full name
     */
    public FullName toFullName(NameAddress applicant) {
        FullName fullName = new FullName();
        fullName.setTitle(applicant.getTitle());
        fullName.setFirstForename(applicant.getForename1());
        fullName.setSecondForename(applicant.getForename2());
        fullName.setThirdForename(applicant.getForename3());
        fullName.setSurname(applicant.getSurname());
        return fullName;
    }

    /**
     * to contact details.
     *
     * @param applicant The standard applicant name address
     * @return The contact details
     */
    public ContactDetails toContactDetails(NameAddress applicant) {
        ContactDetails contactDetails = new ContactDetails();
        if (applicant != null) {
            contactDetails.setAddressLine1(applicant.getAddress1());
            contactDetails.setAddressLine2(applicant.getAddress2());
            contactDetails.setAddressLine3(applicant.getAddress3());
            contactDetails.setAddressLine4(applicant.getAddress4());
            contactDetails.setAddressLine5(applicant.getAddress5());
            contactDetails.setEmail(applicant.getEmailAddress());
            contactDetails.setMobile(applicant.getMobileNumber());
            contactDetails.setPhone(applicant.getTelephoneNumber());
            contactDetails.setPostcode(applicant.getPostcode());
        }
        return contactDetails;
    }

    @Mapping(target = "title", source = "person.name.title")
    @Mapping(target = "surname", source = "person.name.surname")
    @Mapping(target = "forename1", source = "person.name.firstForename")
    @Mapping(target = "forename2", source = "person.name.secondForename")
    @Mapping(target = "forename3", source = "person.name.thirdForename")
    @Mapping(target = "address1", source = "person.contactDetails.addressLine1")
    @Mapping(target = "address2", source = "person.contactDetails.addressLine2")
    @Mapping(target = "address3", source = "person.contactDetails.addressLine3")
    @Mapping(target = "address4", source = "person.contactDetails.addressLine4")
    @Mapping(target = "address5", source = "person.contactDetails.addressLine5")
    @Mapping(target = "postcode", source = "person.contactDetails.postcode")
    @Mapping(target = "telephoneNumber", source = "person.contactDetails.phone")
    @Mapping(target = "mobileNumber", source = "person.contactDetails.mobile")
    @Mapping(target = "emailAddress", source = "person.contactDetails.email")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "name", ignore = true)
    abstract NameAddress toPerson(Person person);

    @Mapping(target = "name", source = "organisation.name")
    @Mapping(target = "address1", source = "organisation.contactDetails.addressLine1")
    @Mapping(target = "address2", source = "organisation.contactDetails.addressLine2")
    @Mapping(target = "address3", source = "organisation.contactDetails.addressLine3")
    @Mapping(target = "address4", source = "organisation.contactDetails.addressLine4")
    @Mapping(target = "address5", source = "organisation.contactDetails.addressLine5")
    @Mapping(target = "postcode", source = "organisation.contactDetails.postcode")
    @Mapping(target = "telephoneNumber", source = "organisation.contactDetails.phone")
    @Mapping(target = "mobileNumber", source = "organisation.contactDetails.mobile")
    @Mapping(target = "emailAddress", source = "organisation.contactDetails.email")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "surname", ignore = true)
    @Mapping(target = "forename1", ignore = true)
    @Mapping(target = "forename2", ignore = true)
    @Mapping(target = "forename3", ignore = true)
    abstract NameAddress toOrganisation(Organisation organisation);

    /**
     * Generates the name address from an applicant.
     *
     * @param applicant The applicant details
     * @return The name address
     */
    public NameAddress toApplicantNameAddress(Applicant applicant) {
        if (applicant != null && applicant.getPerson() != null) {
            return toPerson(applicant.getPerson());
        } else if (applicant != null && applicant.getOrganisation() != null) {
            return toOrganisation(applicant.getOrganisation());
        } else {
            return null;
        }
    }

    /**
     * Generates the name address from an respondent.
     *
     * @param applicant The applicant details
     * @return The name address
     */
    public NameAddress toRespondentNameAddress(Respondent applicant) {
        if (applicant != null && applicant.getPerson() != null) {
            NameAddress nameAddress = toPerson(applicant.getPerson());
            nameAddress.setDateOfBirth(applicant.getPerson().getDateOfBirth());
            return nameAddress;
        } else if (applicant != null && applicant.getOrganisation() != null) {
            return toOrganisation(applicant.getOrganisation());
        } else {
            return null;
        }
    }

    /**
     * Maps the respondent to a name address.
     *
     * @param respondent The respondent details
     * @return The mapped entity
     */
    public NameAddress toRespondent(Respondent respondent) {
        NameAddress nameAddress = toRespondentNameAddress(respondent);
        nameAddress.setCode(NameAddress.RESPONDENT_CODE);
        return nameAddress;
    }

    /**
     * There is a one to one between applicant and standard applicant. Map the values directly.
     *
     * @param standardApplicant The standard applicant
     * @return The name address entity representation
     */
    @Mapping(target = "code", source = "applicantCode")
    @Mapping(target = "title", source = "applicantTitle")
    @Mapping(target = "forename1", source = "applicantForename1")
    @Mapping(target = "forename2", source = "applicantForename2")
    @Mapping(target = "forename3", source = "applicantForename3")
    @Mapping(target = "surname", source = "applicantSurname")
    @Mapping(target = "address1", source = "addressLine1")
    @Mapping(target = "address2", source = "addressLine2")
    @Mapping(target = "address3", source = "addressLine3")
    @Mapping(target = "address4", source = "addressLine4")
    @Mapping(target = "address5", source = "addressLine5")
    @Mapping(target = "userName", source = "createdUser")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    public abstract NameAddress toApplicantEntity(StandardApplicant standardApplicant);
}
