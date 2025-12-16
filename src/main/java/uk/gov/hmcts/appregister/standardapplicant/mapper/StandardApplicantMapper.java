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

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(
            target = "applicant",
            expression =
                    "java(applicantMapper.toApplicant(applicantMapper.toApplicantEntity(entity)))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(StandardApplicant entity);

    @Mapping(target = "code", source = "applicantCode")
    @Mapping(
            target = "applicant",
            expression =
                    "java(applicantMapper.toApplicant(applicantMapper.toApplicantEntity(entity)))")
    @Mapping(target = "startDate", source = "applicantStartDate")
    @Mapping(target = "endDate", source = "applicantEndDate", qualifiedByName = "toEndDate")
    public abstract StandardApplicantGetDetailDto toReadGetDto(StandardApplicant entity);

    @Named("toEndDate")
    static JsonNullable<LocalDate> toEndDate(LocalDate date) {
        if (date != null) {
            return JsonNullable.of(date);
        } else {
            return JsonNullable.undefined();
        }
    }
}
