package uk.gov.hmcts.appregister.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.read.ApplicationFeeRecordDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationWriteDto;
import uk.gov.hmcts.appregister.model.Application;
import uk.gov.hmcts.appregister.model.ApplicationFee;
import uk.gov.hmcts.appregister.model.ApplicationFeeRecord;
import uk.gov.hmcts.appregister.service.VersionManager;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class ApplicationFeeRecordMapper {

    private final VersionManager versionManager;

    public ApplicationFeeRecordDto toReadDto(ApplicationFeeRecord entity) {
        return new ApplicationFeeRecordDto(
            entity.getId(),
            entity.getPaymentReference(),
            entity.getFeeStatus(),
            entity.getStatusDate(),
            entity.getCreationDate(),
            entity.getApplicationFee().getAmount(),
            entity.getApplicationFee().getDescription()
        );
    }

    public ApplicationFeeRecord createEntity(
        ApplicationWriteDto dto,
        Application application,
        ApplicationFee fee,
        String userName,
        LocalDate changedDate
    ) {
        return ApplicationFeeRecord.builder()
            .application(application)
            .applicationFee(fee)
            .feeStatus(dto.feeTypeStatus())
            .statusDate(changedDate)
            .paymentReference(dto.paymentRef())
            .version(versionManager.increment(0))
            .changedBy(userName)
            .changedDate(changedDate)
            .userName(userName)
            .build();
    }
}
