package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.mapper.ApplicantMapper;
import uk.gov.hmcts.appregister.common.mapper.OutgoingDtoSanitiser;
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
public abstract class StandardApplicantMapper {

    private ApplicantMapper applicantMapper;

    @Autowired
    public void setApplicantMapper(ApplicantMapper applicantMapper) {
        this.applicantMapper = applicantMapper;
    }

    protected ApplicantMapper getApplicantMapper() {
        return applicantMapper;
    }

    @Mapping(target = "code", source = "standardApplicant.applicantCode")
    @Mapping(target = "applicant", expression = "java(mapApplicantFromProjection(projection))")
    @Mapping(target = "startDate", source = "standardApplicant.applicantStartDate")
    @Mapping(
            target = "endDate",
            expression = "java(toEndDate(projection.getStandardApplicant().getApplicantEndDate()))")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(
            StandardApplicantEnrichedProjection projection);

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(
            target = "applicant",
            expression =
                    "java(getApplicantMapper().toApplicant("
                            + "getApplicantMapper().toApplicantEntity(entity)))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", expression = "java(toEndDate(entity.getApplicantEndDate()))")
    public abstract StandardApplicantGetDetailDto toReadGetDto(StandardApplicant entity);

    @AfterMapping
    protected void sanitizeSummaryDto(@MappingTarget StandardApplicantGetSummaryDto target) {
        OutgoingDtoSanitiser.sanitize(target);
    }

    @AfterMapping
    protected void sanitizeDetailDto(@MappingTarget StandardApplicantGetDetailDto target) {
        OutgoingDtoSanitiser.sanitize(target);
    }

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicantCode", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "applicantStartDate", source = "from")
    @Mapping(target = "applicantEndDate", source = "to")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "applicantTitle", ignore = true)
    @Mapping(target = "applicantForename1", ignore = true)
    @Mapping(target = "applicantForename2", ignore = true)
    @Mapping(target = "applicantForename3", ignore = true)
    @Mapping(target = "applicantSurname", ignore = true)
    @Mapping(target = "addressLine1", source = "addressLine1")
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
    @Mapping(target = "applicantStartDate", ignore = true)
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
    public abstract StandardApplicant toEntity(String code);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateEntity(
            @MappingTarget StandardApplicant existing, StandardApplicant sa);

    @Named("toEndDate")
    static JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date != null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.of(null);
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
                    OutgoingDtoSanitiser.emptyToNull(
                            projection.getEffectiveName() != null
                                    ? projection.getEffectiveName()
                                    : standardApplicant.getName()));
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
        String middleName =
                ApplicantMapper.combineMiddleName(
                        standardApplicant.getApplicantForename2(),
                        standardApplicant.getApplicantForename3());

        FullName name = new FullName();
        name.setTitle(OutgoingDtoSanitiser.emptyToNull(standardApplicant.getApplicantTitle()));
        name.setFirstName(
                OutgoingDtoSanitiser.emptyToNull(standardApplicant.getApplicantForename1()));
        name.setMiddleName(
                middleName == null ? JsonNullable.of(null) : JsonNullable.of(middleName));
        name.setLastName(OutgoingDtoSanitiser.emptyToNull(standardApplicant.getApplicantSurname()));
        return name;
    }

    protected ContactDetails mapContactDetailsFromStandardApplicant(
            StandardApplicant standardApplicant) {
        ContactDetails contactDetails = new ContactDetails();

        contactDetails.setAddressLine1(
                OutgoingDtoSanitiser.emptyToNull(standardApplicant.getAddressLine1()));

        contactDetails.setAddressLine2(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getAddressLine2())));

        contactDetails.setAddressLine3(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getAddressLine3())));

        contactDetails.setAddressLine4(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getAddressLine4())));

        contactDetails.setAddressLine5(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getAddressLine5())));

        contactDetails.setPostcode(
                OutgoingDtoSanitiser.emptyToNull(standardApplicant.getPostcode()));

        contactDetails.setPhone(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getTelephoneNumber())));

        contactDetails.setMobile(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getMobileNumber())));

        contactDetails.setEmail(
                JsonNullable.of(
                        OutgoingDtoSanitiser.emptyToNull(standardApplicant.getEmailAddress())));

        return contactDetails;
    }
}
