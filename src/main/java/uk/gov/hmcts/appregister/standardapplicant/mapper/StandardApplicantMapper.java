package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import lombok.Setter;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.generated.model.Applicant;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.FullName;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Person;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;

/**
 * Mapper for StandardApplicant entity to StandardApplicantDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Setter
public abstract class StandardApplicantMapper {

    @Autowired ApplicantMapper applicantMapper;

    @Mapping(target = "code", source = "standardApplicant.applicantCode")
    @Mapping(target = "applicant", expression = "java(mapApplicantFromProjection(projection))")
    @Mapping(target = "startDate", source = "standardApplicant.applicantStartDate")
    @Mapping(
            target = "endDate",
            source = "standardApplicant.applicantEndDate",
            qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(
            StandardApplicantEnrichedProjection projection);

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(
            target = "applicant",
            expression =
                    "java(applicantMapper.toApplicant(applicantMapper.toApplicantEntity(entity)))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetDetailDto toReadGetDto(StandardApplicant entity);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicantCode", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "applicantStartDate", ignore = true)
    @Mapping(target = "applicantEndDate", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "applicantTitle", ignore = true)
    @Mapping(target = "applicantForename1", ignore = true)
    @Mapping(target = "applicantForename2", ignore = true)
    @Mapping(target = "applicantForename3", ignore = true)
    @Mapping(target = "applicantSurname", ignore = true)
    @Mapping(target = "addressLine1", ignore = true)
    @Mapping(target = "addressLine2", ignore = true)
    @Mapping(target = "addressLine3", ignore = true)
    @Mapping(target = "addressLine4", ignore = true)
    @Mapping(target = "addressLine5", ignore = true)
    @Mapping(target = "postcode", ignore = true)
    @Mapping(target = "emailAddress", ignore = true)
    @Mapping(target = "telephoneNumber", ignore = true)
    @Mapping(target = "mobileNumber", ignore = true)
    public abstract StandardApplicant toEntity(CodeAndName codeAndName);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicantCode", source = "code")
    @Mapping(target = "applicantStartDate", source = "date")
    @Mapping(target = "applicantEndDate", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "applicantTitle", ignore = true)
    @Mapping(target = "applicantForename1", ignore = true)
    @Mapping(target = "applicantForename2", ignore = true)
    @Mapping(target = "applicantForename3", ignore = true)
    @Mapping(target = "applicantSurname", ignore = true)
    @Mapping(target = "addressLine1", ignore = true)
    @Mapping(target = "addressLine2", ignore = true)
    @Mapping(target = "addressLine3", ignore = true)
    @Mapping(target = "addressLine4", ignore = true)
    @Mapping(target = "addressLine5", ignore = true)
    @Mapping(target = "postcode", ignore = true)
    @Mapping(target = "emailAddress", ignore = true)
    @Mapping(target = "telephoneNumber", ignore = true)
    @Mapping(target = "mobileNumber", ignore = true)
    public abstract StandardApplicant toEntity(String code, LocalDate date);

    @Named("toEndDate")
    static JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date != null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.undefined();
        }
    }

    protected Applicant mapApplicantFromProjection(StandardApplicantEnrichedProjection projection) {
        Applicant applicant = new Applicant();

        StandardApplicant standardApplicant = projection.getStandardApplicant();
        if (standardApplicant == null) {
            return applicant;
        }

        if (StandardApplicant.isOrganisation(standardApplicant)) {
            Organisation organisation = new Organisation();
            organisation.setName(
                    projection.getEffectiveName() != null
                            ? projection.getEffectiveName()
                            : standardApplicant.getName());
            organisation.setContactDetails(
                    mapContactDetailsFromStandardApplicant(standardApplicant));
            applicant.setOrganisation(organisation);
        } else if (standardApplicant.getApplicantForename1() != null
                && standardApplicant.getApplicantSurname() != null) {
            FullName name = getFullName(standardApplicant);

            Person person = new Person();
            person.setName(name);
            person.setContactDetails(mapContactDetailsFromStandardApplicant(standardApplicant));

            applicant.setPerson(person);
        }

        return applicant;
    }

    private static FullName getFullName(StandardApplicant standardApplicant) {
        FullName name = new FullName();
        name.setTitle(standardApplicant.getApplicantTitle());
        name.setFirstForename(standardApplicant.getApplicantForename1());
        name.setSecondForename(
                standardApplicant.getApplicantForename2() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getApplicantForename2()));
        name.setThirdForename(
                standardApplicant.getApplicantForename3() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getApplicantForename3()));
        name.setSurname(standardApplicant.getApplicantSurname());
        return name;
    }

    protected ContactDetails mapContactDetailsFromStandardApplicant(
            StandardApplicant standardApplicant) {
        ContactDetails contactDetails = new ContactDetails();

        contactDetails.setAddressLine1(standardApplicant.getAddressLine1());

        contactDetails.setAddressLine2(
                standardApplicant.getAddressLine2() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getAddressLine2()));

        contactDetails.setAddressLine3(
                standardApplicant.getAddressLine3() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getAddressLine3()));

        contactDetails.setAddressLine4(
                standardApplicant.getAddressLine4() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getAddressLine4()));

        contactDetails.setAddressLine5(
                standardApplicant.getAddressLine5() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getAddressLine5()));

        contactDetails.setPostcode(standardApplicant.getPostcode());

        contactDetails.setPhone(
                standardApplicant.getTelephoneNumber() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getTelephoneNumber()));

        contactDetails.setMobile(
                standardApplicant.getMobileNumber() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getMobileNumber()));

        contactDetails.setEmail(
                standardApplicant.getEmailAddress() == null
                        ? JsonNullable.undefined()
                        : JsonNullable.of(standardApplicant.getEmailAddress()));

        return contactDetails;
    }
}
