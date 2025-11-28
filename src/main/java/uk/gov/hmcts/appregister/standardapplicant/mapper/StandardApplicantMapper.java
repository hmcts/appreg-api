package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

/**
 * Mapper for StandardApplicant entity to StandardApplicantDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class StandardApplicantMapper {

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(target = "applicant", expression = "java(toApplicant(entity))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(StandardApplicant entity);

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(target = "applicant", expression = "java(toApplicant(entity))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetDetailDto toReadGetDto(StandardApplicant entity);

    @Deprecated
    @Mapping(target = "applicantName", source = "name")
    public abstract StandardApplicantDto toReadDto(StandardApplicant entity);

    /**
     * A useful mapper to map the applicant details of the standard applicant.
     *
     * @param applicant The database applicant
     * @return The applicant Dto
     */
    public Applicant toApplicant(StandardApplicant applicant) {
        Applicant applicantDto = new Applicant();

        ContactDetails contactDetails = toContactDetails(applicant);

        // if the name is set then this is an organisation otherwise a person
        if (StandardApplicant.isOrganisation(applicant)) {
            Organisation organisation = new Organisation();
            organisation.setName(applicant.getName());
            organisation.setContactDetails(contactDetails);
            applicantDto.setOrganisation(organisation);

            applicantDto.setOrganisation(organisation);
        } else {
            FullName fullName = toFullName(applicant);
            Person person = new Person();
            person.setContactDetails(contactDetails);
            person.setName(fullName);

            applicantDto.setPerson(person);
        }

        return applicantDto;
    }

    /**
     * to full name.
     *
     * @param applicant The standard applicant
     * @return The full name
     */
    FullName toFullName(StandardApplicant applicant) {
        FullName fullName = new FullName();
        fullName.setTitle(applicant.getApplicantTitle());
        fullName.setFirstForename(applicant.getApplicantForename1());
        fullName.setSecondForename(applicant.getApplicantForename2());
        fullName.setThirdForename(applicant.getApplicantForename3());
        fullName.setSurname(applicant.getApplicantSurname());
        return fullName;
    }

    /**
     * to contact details.
     *
     * @param applicant The standard applicant
     * @return The contact details
     */
    ContactDetails toContactDetails(StandardApplicant applicant) {
        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setAddressLine1(applicant.getAddressLine1());
        contactDetails.setAddressLine2(applicant.getAddressLine2());
        contactDetails.setAddressLine3(applicant.getAddressLine3());
        contactDetails.setAddressLine4(applicant.getAddressLine4());
        contactDetails.setAddressLine5(applicant.getAddressLine5());
        contactDetails.setEmail(applicant.getEmailAddress());
        contactDetails.setMobile(applicant.getMobileNumber());
        contactDetails.setPhone(applicant.getTelephoneNumber());
        contactDetails.setPostcode(applicant.getPostcode());
        return contactDetails;
    }

    @Named("toEndDate")
    static JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date != null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.undefined();
        }
    }
}
