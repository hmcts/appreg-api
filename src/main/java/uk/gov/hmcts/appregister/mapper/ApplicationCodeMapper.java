package uk.gov.hmcts.appregister.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.internal.FeePair;
import uk.gov.hmcts.appregister.dto.read.ApplicationCodeDto;
import uk.gov.hmcts.appregister.model.ApplicationCode;
import uk.gov.hmcts.appregister.model.ApplicationFee;

@Component
public class ApplicationCodeMapper {

    public ApplicationCodeDto toReadDto(ApplicationCode entity, FeePair fees) {
        if (entity == null) return null;

        ApplicationFee mainFee = fees != null ? fees.mainFee() : null;
        ApplicationFee offsetFee = fees != null ? fees.offsetFee() : null;

        return new ApplicationCodeDto(
            entity.getId(),
            entity.getApplicationCode(),
            entity.getTitle(),
            entity.getWording(),
            entity.getLegislation(),
            entity.getFeeDue(),
            entity.getRequiresRespondent(),
            entity.getDestinationEmail1(),
            entity.getDestinationEmail2(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getBulkRespondentAllowed(),
            entity.getFeeReference(),
            mainFee != null ? mainFee.getDescription() : null,
            mainFee != null ? mainFee.getAmount() : null,
            offsetFee != null ? offsetFee.getDescription() : null,
            offsetFee != null ? offsetFee.getAmount() : null
        );
    }

    public ApplicationCode toEntityFromReadDto(ApplicationCodeDto dto) {
        if (dto == null) return null;

        return ApplicationCode.builder()
            .id(dto.id())
            .applicationCode(dto.applicationCode())
            .title(dto.title())
            .wording(dto.wording())
            .legislation(dto.legislation())
            .feeDue(dto.feeDue())
            .requiresRespondent(dto.requiresRespondent())
            .destinationEmail1(dto.destinationEmail1())
            .destinationEmail2(dto.destinationEmail2())
            .startDate(dto.startDate())
            .endDate(dto.endDate())
            .bulkRespondentAllowed(dto.bulkRespondentAllowed())
            .feeReference(dto.feeReference())
            .build();
    }
}
