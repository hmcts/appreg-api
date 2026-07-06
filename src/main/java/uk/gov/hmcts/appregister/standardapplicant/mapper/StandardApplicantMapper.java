package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import java.util.List;
import org.mapstruct.AfterMapping;
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
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintRowDto;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;

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

    @Mapping(target = "applicantCode", source = "applicantCode")
    @Mapping(target = "applicantTitle", ignore = true)
    @Mapping(target = "name", source = "name")
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
    @Mapping(target = "applicantStartDate", source = "applicantStartDate")
    @Mapping(target = "applicantEndDate", source = "applicantEndDate")
    public abstract List<StandardApplicantCsvRow> toEntity(
            List<StandardApplicant> standardApplicants);

    @Mapping(target = "code", source = "standardApplicant.applicantCode")
    @Mapping(target = "useFrom", source = "standardApplicant.applicantStartDate")
    @Mapping(target = "name", source = "standardApplicant.name")
    @Mapping(target = "useTo", source = "standardApplicant.applicantEndDate")
    @Mapping(target = "title", source = "standardApplicant.applicantTitle")
    @Mapping(target = "addressLine1", source = "standardApplicant.addressLine1")
    @Mapping(target = "forename1", source = "standardApplicant.applicantForename1")
    @Mapping(target = "addressLine2", source = "standardApplicant.addressLine2")
    @Mapping(target = "forename2", source = "standardApplicant.applicantForename2")
    @Mapping(target = "addressLine3", source = "standardApplicant.addressLine3")
    @Mapping(target = "forename3", source = "standardApplicant.applicantForename3")
    @Mapping(target = "addressLine4", source = "standardApplicant.addressLine4")
    @Mapping(target = "surname", source = "standardApplicant.applicantSurname")
    @Mapping(target = "addressLine5", source = "standardApplicant.addressLine5")
    @Mapping(target = "emailAddress", source = "standardApplicant.emailAddress")
    @Mapping(target = "postcode", source = "standardApplicant.postcode")
    @Mapping(target = "telephoneNumber", source = "standardApplicant.telephoneNumber")
    @Mapping(target = "mobileNumber", source = "standardApplicant.mobileNumber")
    public abstract StandardApplicantPrintRowDto toPrintRowDto(
            StandardApplicantEnrichedProjection projection);

    @AfterMapping
    protected void ensureRequiredPrintRowFieldsArePresent(
            @MappingTarget StandardApplicantPrintRowDto target) {
        target.setCode(requiredNullable(target.getCode()));
        target.setUseFrom(requiredNullable(target.getUseFrom()));
        target.setName(requiredNullable(target.getName()));
        target.setUseTo(requiredNullable(target.getUseTo()));
        target.setTitle(requiredNullable(target.getTitle()));
        target.setAddressLine1(requiredNullable(target.getAddressLine1()));
        target.setForename1(requiredNullable(target.getForename1()));
        target.setAddressLine2(requiredNullable(target.getAddressLine2()));
        target.setForename2(requiredNullable(target.getForename2()));
        target.setAddressLine3(requiredNullable(target.getAddressLine3()));
        target.setForename3(requiredNullable(target.getForename3()));
        target.setAddressLine4(requiredNullable(target.getAddressLine4()));
        target.setSurname(requiredNullable(target.getSurname()));
        target.setAddressLine5(requiredNullable(target.getAddressLine5()));
        target.setEmailAddress(requiredNullable(target.getEmailAddress()));
        target.setPostcode(requiredNullable(target.getPostcode()));
        target.setTelephoneNumber(requiredNullable(target.getTelephoneNumber()));
        target.setMobileNumber(requiredNullable(target.getMobileNumber()));
    }

    private static <T> JsonNullable<T> requiredNullable(JsonNullable<T> value) {
        return value != null && value.isPresent() ? value : JsonNullable.of(null);
    }

    protected JsonNullable<String> map(String value) {
        return JsonNullable.of(OutgoingDtoSanitiser.emptyToNull(value));
    }

    protected JsonNullable<LocalDate> map(LocalDate value) {
        return JsonNullable.of(value);
    }

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
