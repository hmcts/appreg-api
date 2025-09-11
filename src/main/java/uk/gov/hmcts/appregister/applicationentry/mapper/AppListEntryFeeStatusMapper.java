package uk.gov.hmcts.appregister.applicationentry.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationentry.dto.AppListEntryFeeStatusDto;
import uk.gov.hmcts.appregister.applicationentry.dto.ApplicationWriteDto;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;

@RequiredArgsConstructor
@Component
public class AppListEntryFeeStatusMapper {

    public AppListEntryFeeStatusDto toReadDto(AppListEntryFeeStatus entity) {
        return new AppListEntryFeeStatusDto(
                entity.getId(),
                entity.getAlefsPaymentReference(),
                FeeStatusType.fromDisplayName(entity.getAlefsFeeStatus()),
                entity.getAlefsFeeStatusDate(),
                entity.getAlefsStatusCreationDate(),
                entity.getAppListEntry().getEntryFeeIds().stream()
                        .findFirst()
                        .get()
                        .getFeeId()
                        .getAmount(),
                entity.getAppListEntry().getEntryFeeIds().stream()
                        .findFirst()
                        .get()
                        .getFeeId()
                        .getDescription());
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
