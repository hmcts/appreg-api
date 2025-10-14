package uk.gov.hmcts.appregister.applicationentry.mapper;

import java.util.Optional;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.dto.AppListEntryFeeStatusDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeId;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

@Component
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class AppListEntryFeeStatusMapper {

    @Mapping(target = "paymentReference", source = "alefsPaymentReference")
    @Mapping(target = "feeStatus", source = "alefsFeeStatus", qualifiedByName = "mapFeeStatus")
    @Mapping(target = "amount", source = "appListEntry", qualifiedByName = "entryToAmount")
    @Mapping(
            target = "feeDescription",
            source = "appListEntry",
            qualifiedByName = "statusToDescription")
    @Mapping(target = "statusDate", source = "alefsFeeStatusDate")
    @Mapping(target = "creationDate", source = "alefsStatusCreationDate")
    public abstract AppListEntryFeeStatusDto toReadDto(AppListEntryFeeStatus entity);

    @Named("mapFeeStatus")
    public FeeStatusType mapFeeStatus(String alefsFeeStatus) {
        return FeeStatusType.fromDisplayName(alefsFeeStatus);
    }

    @Named("entryToAmount")
    public Double entryToAmount(ApplicationListEntry entry) {
        Optional<AppListEntryFeeId> doubleOptional = entry.getEntryFeeIds().stream().findFirst();
        return doubleOptional
                .map(appListEntryFeeId -> appListEntryFeeId.getFeeId().getAmount())
                .orElse(null);
    }

    @Named("statusToDescription")
    public String statusToDescription(ApplicationListEntry entity) {
        Optional<AppListEntryFeeId> doubleOptional = entity.getEntryFeeIds().stream().findFirst();
        return doubleOptional
                .map(appListEntryFeeId -> appListEntryFeeId.getFeeId().getDescription())
                .orElse(null);
    }

    @SuppressWarnings("java:S1135")
    public AppListEntryFeeStatus createEntity(
            ApplicationWriteDto dto, ApplicationListEntry application) {
        return AppListEntryFeeStatus.builder()
                .appListEntry(application)

                // TODO: handle multiple fees
                // applicationFee(fee.getAmount())
                .alefsFeeStatus(dto.feeTypeStatus().getDisplayName())
                .alefsPaymentReference(dto.paymentRef())
                .build();
    }
}
