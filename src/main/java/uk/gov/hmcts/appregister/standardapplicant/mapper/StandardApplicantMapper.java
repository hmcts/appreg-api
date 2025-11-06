package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.mapstruct.*;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.standardapplicant.dto.StandardApplicantDto;

import java.time.LocalDate;

/**
 * Mapper for StandardApplicant entity to StandardApplicantDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StandardApplicantMapper {
    @Mapping(target = "code", source = "applicantCode")
    @Mapping(target = "fullName.title", source = "applicantTitle")
    @Mapping(target = "fullName.surname", source = "applicantSurname")
    @Mapping(target = "fullName.firstForename", source = "applicantForename1")
    @Mapping(target = "fullName.secondForename", source = "applicantForename2")
    @Mapping(target = "fullName.thirdForename", source = "applicantForename3")
    @Mapping(target = "contactDetails.addressLine1", source = "addressLine1")
    @Mapping(target = "contactDetails.addressLine2", source = "addressLine2")
    @Mapping(target = "contactDetails.addressLine3", source = "addressLine3")
    @Mapping(target = "contactDetails.addressLine4", source = "addressLine4")
    @Mapping(target = "contactDetails.addressLine5", source = "addressLine5")
    @Mapping(target = "contactDetails.postcode", source = "postcode")
    @Mapping(target = "contactDetails.phone", source = "telephoneNumber")
    @Mapping(target = "contactDetails.mobile", source = "mobileNumber")
    @Mapping(target = "contactDetails.email", source = "emailAddress")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    StandardApplicantGetDetailDto toReadGetDto(StandardApplicant entity);

    @Deprecated
    @Mapping(target = "applicantName", source = "name")
    StandardApplicantDto toReadDto(StandardApplicant entity);

    @Named("toEndDate")
    static JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date == null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.undefined();
        }
    }
}
