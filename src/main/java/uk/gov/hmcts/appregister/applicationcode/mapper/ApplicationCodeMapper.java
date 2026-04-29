package uk.gov.hmcts.appregister.applicationcode.mapper;

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
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.model.PayloadForGet;
import uk.gov.hmcts.appregister.common.util.CurrencyUtil;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoOffsiteFeeAmount;

/**
 * Mapper for ApplicationCode entity and ApplicationCodeDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Setter
public abstract class ApplicationCodeMapper {

    @Autowired WordingTemplateMapper wordingTemplateMapper;

    /**
     * A fee to dto mapping rule.
     *
     * @param fee Maps a fee to the dto.
     * @return the fee amount dto
     */
    @Named("mapFee")
    public JsonNullable<ApplicationCodeGetSummaryDtoFeeAmount> map(Fee fee) {
        long pence = CurrencyUtil.getPennies(fee);

        ApplicationCodeGetSummaryDtoFeeAmount dto = new ApplicationCodeGetSummaryDtoFeeAmount();
        dto.setValue(pence);
        dto.setCurrency(ApplicationCodeGetSummaryDtoFeeAmount.CurrencyEnum.GBP);

        return JsonNullable.of(dto);
    }

    /**
     * A yes or no to boolean mapping rule.
     *
     * @param yesOrNo Maps yes or no to boolean.
     * @return the fee amount dto
     */
    public boolean map(YesOrNo yesOrNo) {
        return yesOrNo.isYes();
    }

    public JsonNullable<String> map(String str) {
        return JsonNullable.of(str);
    }

    @Named("mapOffsite")
    public JsonNullable<ApplicationCodeGetSummaryDtoOffsiteFeeAmount> mapOffsite(Fee fee) {
        long pence = CurrencyUtil.getPennies(fee);

        ApplicationCodeGetSummaryDtoOffsiteFeeAmount dto =
                new ApplicationCodeGetSummaryDtoOffsiteFeeAmount();
        dto.setValue(pence);
        dto.setCurrency(ApplicationCodeGetSummaryDtoOffsiteFeeAmount.CurrencyEnum.GBP);

        return JsonNullable.of(dto);
    }

    @Named("mapFeeReference")
    public JsonNullable<String> mapFeeReference(String feeReference) {
        return JsonNullable.of(feeReference);
    }

    @Named("mapNullableLocalDate")
    public JsonNullable<LocalDate> mapNullableLocalDate(LocalDate localDate) {
        return (localDate == null) ? JsonNullable.undefined() : JsonNullable.of(localDate);
    }

    @Mapping(target = "offsiteFeeAmount", source = "offsiteFee", qualifiedByName = "mapOffsite")
    @Mapping(target = "feeAmount", source = "fee", qualifiedByName = "mapFee")
    @Mapping(target = "applicationCode", source = "entity.code")
    @Mapping(target = "title", source = "entity.title")
    @Mapping(
            target = "wording",
            expression =
                    "java(wordingTemplateMapper.getTemplateDetail(() -> entity.getWording(), null))")
    @Mapping(target = "requiresRespondent", source = "entity.requiresRespondent")
    @Mapping(target = "bulkRespondentAllowed", source = "entity.bulkRespondentAllowed")
    @Mapping(target = "feeReference", source = "fee.reference", qualifiedByName = "mapFeeReference")
    @Mapping(target = "feeDescription", source = "fee.description")
    @Mapping(target = "isFeeDue", source = "entity.feeDue")
    @Mapping(
            target = "offsiteFeeReference",
            source = "offsiteFee.reference",
            qualifiedByName = "mapFeeReference")
    @Mapping(target = "offsiteFeeDescription", source = "offsiteFee.description")

    /**
     * maps the application code entity to summary dto.
     *
     * @param entity the application code entity
     * @param fee the fee (main fee)*
     * @param offsiteFee the offsite fee
     * @return The application code detail dto
     */
    public abstract ApplicationCodeGetSummaryDto toApplicationCodeGetSummaryDto(
            ApplicationCode entity, Fee fee, Fee offsiteFee);

    /**
     * maps the application code entity to detail dto.
     *
     * @param entity the application code entity
     * @param fee the fee (main fee)*
     * @param offsiteFee the offsite fee
     * @return The application code detail dto
     */
    @Mapping(target = "offsiteFeeAmount", source = "offsiteFee", qualifiedByName = "mapOffsite")
    @Mapping(target = "feeAmount", source = "fee", qualifiedByName = "mapFee")
    @Mapping(target = "applicationCode", source = "entity.code")
    @Mapping(target = "title", source = "entity.title")
    @Mapping(
            target = "wording",
            expression =
                    "java(wordingTemplateMapper.getTemplateDetail(() -> entity.getWording(), null))")
    @Mapping(target = "requiresRespondent", source = "entity.requiresRespondent")
    @Mapping(target = "bulkRespondentAllowed", source = "entity.bulkRespondentAllowed")
    @Mapping(target = "feeReference", source = "fee.reference", qualifiedByName = "mapFeeReference")
    @Mapping(target = "startDate", source = "entity.startDate")
    @Mapping(
            target = "endDate",
            source = "entity.endDate",
            qualifiedByName = "mapNullableLocalDate")
    @Mapping(target = "feeDescription", source = "fee.description")
    @Mapping(target = "isFeeDue", source = "entity.feeDue")
    @Mapping(
            target = "offsiteFeeReference",
            source = "offsiteFee.reference",
            qualifiedByName = "mapFeeReference")
    @Mapping(target = "offsiteFeeDescription", source = "offsiteFee.description")
    public abstract ApplicationCodeGetDetailDto toApplicationCodeGetDetailDto(
            ApplicationCode entity, Fee fee, Fee offsiteFee);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "title", source = "title")
    public abstract ApplicationCode toEntity(CodeAndTitle record);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "code", source = "payloadForGet.code")
    @Mapping(target = "startDate", source = "payloadForGet.date")
    public abstract ApplicationCode toEntity(PayloadForGet payloadForGet);
}
