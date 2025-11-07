package uk.gov.hmcts.appregister.standardapplicant.mapper;

import org.mapstruct.*;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
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
public abstract class StandardApplicantMapper {
    /** The default name if parts are of the name are null */
    public static String DEFAULT_NAME = "N/K";

    @Mapping(target = "applicantName", source = "name")
    public abstract StandardApplicantDto toReadDto(StandardApplicant entity);

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(target = "name", expression = "java(toNameBasedOnOrganisationVsIndividual(entity))")
    @Mapping(target = "addressLine1", source = "addressLine1")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(StandardApplicant entity);

    /**
     * A useful mapper to map the name depending on the name vs forename and surname data
     * The reason that differentiation exists is to differentiate between organisations and individuals.
     * @param applicant The applicant to map
     * @return The name
     */
    public String toNameBasedOnOrganisationVsIndividual(StandardApplicant applicant) {
        return StandardApplicant.isOrganisation(applicant) ? applicant.getName() : getIndividualName(applicant);
    }

    /**
     * gets the individual name from forename and surname.
     * @param applicant The applicant
     * @return The individual name. At present if the forename and the surname as null then a default
     * string is used
     */
    public String getIndividualName(StandardApplicant applicant) {
        return getForename(applicant) + " " + getSurname(applicant);
    }

    /**
     * gets the forename applying default where applicable.
     * @param applicant The applicant
     * @return The name
     */
    public String getForename(StandardApplicant applicant) {
        return applicant.getApplicantForename1() == null
                ? DEFAULT_NAME
                : applicant.getApplicantForename1();
    }

    /**
     * gets the surname applying default where applicable.
     * @param applicant The applicant
     * @return The name
     */
    public String getSurname(StandardApplicant applicant) {
        return applicant.getApplicantSurname() == null ? DEFAULT_NAME
                : applicant.getApplicantSurname();
    }

    @Named("toEndDate")
    public JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date == null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.undefined();
        }
    }
}

