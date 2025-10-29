package uk.gov.hmcts.appregister.applicationcode.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationCodeGetSummaryDtoFeeAmount;

/**
 * Mapper for ApplicationCode entity and ApplicationCodeDto.
 */
@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ApplicationCodeMapper {

    /**
     * A fee to dto mapping rule.
     *
     * @param fee Maps a fee to the dto.
     * @return the fee amount dto
     */
    public JsonNullable<ApplicationCodeGetSummaryDtoFeeAmount> map(Fee fee) {
        if (fee == null || fee.getAmount() == null) {
            return JsonNullable.undefined();
        }

        // Expecting NUMERIC(9,2) mapped to BigDecimal scale=2
        BigDecimal pounds = fee.getAmount();

        BigDecimal scaled = pounds.setScale(2, RoundingMode.UNNECESSARY);

        long pence = scaled.movePointRight(2).longValueExact();

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

    @Named("mapFeeReference")
    public JsonNullable<String> mapFeeReference(String feeReference) {
        return JsonNullable.of(feeReference);
    }

    @Named("mapNullableLocalDate")
    public JsonNullable<LocalDate> mapNullableLocalDate(LocalDate localDate) {
        return (localDate == null) ? JsonNullable.undefined() : JsonNullable.of(localDate);
    }

    @Mapping(target = "offsiteFeeAmount", source = "offsiteFee")
    @Mapping(target = "feeAmount", source = "fee")
    @Mapping(target = "applicationCode", source = "entity.code")
    @Mapping(target = "title", source = "entity.title")
    @Mapping(target = "wording", source = "entity.wording")
    @Mapping(target = "requiresRespondent", source = "entity.requiresRespondent")
    @Mapping(target = "bulkRespondentAllowed", source = "entity.bulkRespondentAllowed")
    @Mapping(
            target = "feeReference",
            source = "entity.feeReference",
            qualifiedByName = "mapFeeReference")
    @Mapping(target = "feeDescription", source = "fee.description")
    @Mapping(target = "isFeeDue", source = "entity.feeDue")

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
    @Mapping(target = "offsiteFeeAmount", source = "offsiteFee")
    @Mapping(target = "feeAmount", source = "fee")
    @Mapping(target = "applicationCode", source = "entity.code")
    @Mapping(target = "title", source = "entity.title")
    @Mapping(target = "wording", source = "entity.wording")
    @Mapping(target = "requiresRespondent", source = "entity.requiresRespondent")
    @Mapping(target = "bulkRespondentAllowed", source = "entity.bulkRespondentAllowed")
    @Mapping(
            target = "feeReference",
            source = "entity.feeReference",
            qualifiedByName = "mapFeeReference")
    @Mapping(target = "startDate", source = "entity.startDate")
    @Mapping(
            target = "endDate",
            source = "entity.endDate",
            qualifiedByName = "mapNullableLocalDate")
    @Mapping(target = "feeDescription", source = "fee.description")
    @Mapping(target = "isFeeDue", source = "entity.feeDue")
    public abstract ApplicationCodeGetDetailDto toApplicationCodeGetDetailDto(
            ApplicationCode entity, Fee fee, Fee offsiteFee);
}
