package uk.gov.hmcts.appregister.common.mapper;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.enumeration.NameAddressCodeType;
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
        nameAddress.setCode(NameAddressCodeType.APPLICANT);
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
        String firstName = firstNonBlank(applicant.getFirstName(), applicant.getForename1());
        String middleName =
                firstNonBlank(
                        applicant.getMiddleName(),
                        combineMiddleName(applicant.getForename2(), applicant.getForename3()));
        String lastName = firstNonBlank(applicant.getLastName(), applicant.getSurname());

        FullName fullName = new FullName();
        fullName.setTitle(applicant.getTitle());
        fullName.setFirstName(firstName);
        fullName.setMiddleName(JsonNullable.of(middleName));
        fullName.setLastName(lastName);
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
            contactDetails.setAddressLine2(map(applicant.getAddress2()));
            contactDetails.setAddressLine3(map(applicant.getAddress3()));
            contactDetails.setAddressLine4(map(applicant.getAddress4()));
            contactDetails.setAddressLine5(map(applicant.getAddress5()));
            contactDetails.setEmail(map(applicant.getEmailAddress()));
            contactDetails.setMobile(map(applicant.getMobileNumber()));
            contactDetails.setPhone(map(applicant.getTelephoneNumber()));
            contactDetails.setPostcode(applicant.getPostcode());
        }
        return contactDetails;
    }

    NameAddress toPerson(Person person) {
        NameAddress nameAddress = new NameAddress();
        FullName name = person.getName();

        String firstName = firstName(name);
        String middleName = middleName(name);
        String lastName = lastName(name);

        nameAddress.setTitle(name.getTitle());
        nameAddress.setFirstName(firstName);
        nameAddress.setMiddleName(middleName);
        nameAddress.setLastName(lastName);
        nameAddress.setForename1(firstName);
        nameAddress.setForename2(middleName);
        nameAddress.setForename3(null);
        nameAddress.setSurname(lastName);

        ContactDetails contactDetails = person.getContactDetails();
        nameAddress.setAddress1(contactDetails.getAddressLine1());
        nameAddress.setAddress2(map(contactDetails.getAddressLine2()));
        nameAddress.setAddress3(map(contactDetails.getAddressLine3()));
        nameAddress.setAddress4(map(contactDetails.getAddressLine4()));
        nameAddress.setAddress5(map(contactDetails.getAddressLine5()));
        nameAddress.setPostcode(contactDetails.getPostcode());
        nameAddress.setTelephoneNumber(map(contactDetails.getPhone()));
        nameAddress.setMobileNumber(map(contactDetails.getMobile()));
        nameAddress.setEmailAddress(map(contactDetails.getEmail()));

        return nameAddress;
    }

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
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "middleName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
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
        nameAddress.setCode(NameAddressCodeType.RESPONDENT);
        return nameAddress;
    }

    /**
     * There is a one to one between applicant and standard applicant. Map the values directly.
     *
     * @param standardApplicant The standard applicant
     * @return The name address entity representation
     */
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "title", source = "applicantTitle")
    @Mapping(target = "forename1", source = "applicantForename1")
    @Mapping(target = "forename2", source = "applicantForename2")
    @Mapping(target = "forename3", source = "applicantForename3")
    @Mapping(target = "surname", source = "applicantSurname")
    @Mapping(target = "firstName", source = "applicantForename1")
    @Mapping(
            target = "middleName",
            expression =
                    "java(combineMiddleName(standardApplicant.getApplicantForename2(), "
                            + "standardApplicant.getApplicantForename3()))")
    @Mapping(target = "lastName", source = "applicantSurname")
    @Mapping(target = "address1", source = "addressLine1")
    @Mapping(target = "address2", source = "addressLine2")
    @Mapping(target = "address3", source = "addressLine3")
    @Mapping(target = "address4", source = "addressLine4")
    @Mapping(target = "address5", source = "addressLine5")
    @Mapping(target = "userName", source = "createdUser")
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "dmsId", ignore = true)
    public abstract NameAddress toApplicantEntity(StandardApplicant standardApplicant);

    /**
     * Decides the name that should take precedent based on an organisation or person.
     *
     * @param sa The standard applicant to use. This can be null.
     * @param applicant The person to use. This can be null.
     * @return The name that should be used for the applicant or respondent depending. If both are
     *     present then the organisation name will be used. If a person, the name is in the format
     *     firstName lastName. If an organisation the name is used. If all else fails then an empty
     *     string is returned.
     */
    public String getNameForApplicant(StandardApplicant sa, NameAddress applicant) {
        if (sa != null) {

            // if the name is not set i.e. not an org then use person name fields
            if (sa.getName() == null) {
                return formatPersonName(sa.getApplicantForename1(), sa.getApplicantSurname());
            } else {
                return sa.getName();
            }
        } else if (applicant != null) {
            return getNameForNameAddress(applicant);
        }

        // return an empty string
        return "";
    }

    /**
     * gets the name for the name address based on whether the name address has an organisation or
     * not.
     *
     * @param nameAddress The name address to get the name. This can be null.
     * @return The name string for the address in the format firstName lastName if a person or the
     *     name if an organisation. If all else fails then an empty string is returned.
     */
    public String getNameForNameAddress(NameAddress nameAddress) {
        String name = "";
        if (nameAddress != null && nameAddress.getName() == null) {
            name =
                    formatPersonName(
                            firstNonBlank(nameAddress.getFirstName(), nameAddress.getForename1()),
                            firstNonBlank(nameAddress.getLastName(), nameAddress.getSurname()));
        } else if (nameAddress != null) {
            name = nameAddress.getName();
        }
        return name;
    }

    private String formatPersonName(String forename, String surname) {
        if (forename == null && surname == null) {
            return "";
        }
        if (forename == null) {
            return surname;
        }
        if (surname == null) {
            return forename;
        }
        return forename + " " + surname;
    }

    public String map(JsonNullable<String> str) {
        return str != null && str.isPresent() ? str.get() : null;
    }

    public JsonNullable<String> map(String string) {
        return (string != null) ? JsonNullable.of(string) : JsonNullable.of(null);
    }

    public String firstName(FullName name) {
        if (name == null) {
            return null;
        }
        return StringUtils.trimToNull(name.getFirstName());
    }

    public String middleName(FullName name) {
        if (name == null) {
            return null;
        }
        return StringUtils.trimToNull(map(name.getMiddleName()));
    }

    public String lastName(FullName name) {
        if (name == null) {
            return null;
        }
        return StringUtils.trimToNull(name.getLastName());
    }

    public static String combineMiddleName(String secondForename, String thirdForename) {
        return StringUtils.trimToNull(
                String.join(
                        " ",
                        StringUtils.defaultString(StringUtils.trimToNull(secondForename)),
                        StringUtils.defaultString(StringUtils.trimToNull(thirdForename))));
    }

    private String firstNonBlank(String primary, String fallback) {
        String value = StringUtils.trimToNull(primary);
        return value != null ? value : StringUtils.trimToNull(fallback);
    }
}
