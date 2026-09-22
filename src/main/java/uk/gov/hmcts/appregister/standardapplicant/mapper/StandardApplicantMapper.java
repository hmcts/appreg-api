package uk.gov.hmcts.appregister.standardapplicant.mapper;

import java.time.LocalDate;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.mapper.OutgoingDtoSanitiser;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.StandardApplicantPrintRowDto;
import uk.gov.hmcts.appregister.standardapplicant.model.StandardApplicantCsvRow;

/**
 * Maps only the permitted Standard Applicant reference fields to responses.
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class StandardApplicantMapper {

    @Mapping(target = "code", source = "standardApplicant.applicantCode")
    @Mapping(target = "name", source = "standardApplicant.name")
    @Mapping(target = "startDate", source = "standardApplicant.applicantStartDate")
    @Mapping(
            target = "endDate",
            expression = "java(toEndDate(projection.getStandardApplicant().getApplicantEndDate()))")
    public abstract StandardApplicantGetSummaryDto toReadGetSummaryDto(
            StandardApplicantEnrichedProjection projection);

    @Mapping(target = "code", source = "applicantCode")
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

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicantCode", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "addressLine1", source = "addressLine1")
    @Mapping(target = "applicantStartDate", source = "from")
    @Mapping(target = "applicantEndDate", source = "to")
    @Mapping(target = "version", constant = "0L")
    public abstract StandardApplicant toEntity(CodeAndName codeAndName);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "applicantCode", source = "code")
    @Mapping(target = "version", constant = "0L")
    public abstract StandardApplicant toEntity(String code);

    public abstract List<StandardApplicantCsvRow> toEntity(
            List<StandardApplicant> standardApplicants);

    @Mapping(target = "code", source = "standardApplicant.applicantCode")
    @Mapping(target = "useFrom", source = "standardApplicant.applicantStartDate")
    @Mapping(target = "name", source = "standardApplicant.name")
    @Mapping(target = "useTo", source = "standardApplicant.applicantEndDate")
    public abstract StandardApplicantPrintRowDto toPrintRowDto(
            StandardApplicantEnrichedProjection projection);

    @AfterMapping
    protected void ensureRequiredPrintRowFieldsArePresent(
            @MappingTarget StandardApplicantPrintRowDto target) {
        target.setCode(requiredNullable(target.getCode()));
        target.setUseFrom(requiredNullable(target.getUseFrom()));
        target.setName(requiredNullable(target.getName()));
        target.setUseTo(requiredNullable(target.getUseTo()));
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
        return JsonNullable.of(date);
    }
}
