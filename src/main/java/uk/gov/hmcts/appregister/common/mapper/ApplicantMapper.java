package uk.gov.hmcts.appregister.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;

/**
 * A useful mapper to convert standard applicant to applicant Dto.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public class ApplicantMapper {
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
}
