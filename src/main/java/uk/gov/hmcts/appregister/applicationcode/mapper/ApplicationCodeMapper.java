package uk.gov.hmcts.appregister.applicationcode.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationcode.dto.ApplicationCodeDto;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.Fee;
import uk.gov.hmcts.appregister.common.entity.FeePair;

/** Mapper for ApplicationCode entity and ApplicationCodeDto. */
@Component
public class ApplicationCodeMapper {

    public static final String TRUE_VALUE = "1";

    public ApplicationCodeDto toReadDto(ApplicationCode entity, FeePair fees) {
        if (entity == null) {
            return null;
        }

        Fee mainFee = fees != null ? fees.mainFee() : null;
        Fee offsetFee = fees != null ? fees.offsetFee() : null;

        return new ApplicationCodeDto(
                entity.getId(),
                entity.getApplicationCode(),
                entity.getTitle(),
                entity.getWording(),
                entity.getLegislation(),
                (entity.getFeeDue().equals(TRUE_VALUE)),
                entity.getRequiresRespondent().equals(TRUE_VALUE),
                entity.getDestinationEmail1(),
                entity.getDestinationEmail2(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getBulkRespondentAllowed().equals(TRUE_VALUE),
                entity.getFeeReference(),
                mainFee != null ? mainFee.getDescription() : null,
                mainFee != null ? mainFee.getAmount() : null,
                offsetFee != null ? offsetFee.getDescription() : null,
                offsetFee != null ? offsetFee.getAmount() : null);
    }

    public ApplicationCode toEntityFromReadDto(ApplicationCodeDto dto) {
        if (dto == null) {
            return null;
        }

        return ApplicationCode.builder()
                .id(dto.id())
                .applicationCode(dto.applicationCode())
                .title(dto.title())
                .wording(dto.wording())
                .legislation(dto.legislation())
                .feeDue(dto.feeDue().toString())
                .requiresRespondent(dto.requiresRespondent().toString())
                .destinationEmail1(dto.destinationEmail1())
                .destinationEmail2(dto.destinationEmail2())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .bulkRespondentAllowed(dto.bulkRespondentAllowed().toString())
                .feeReference(dto.feeReference())
                .build();
    }
}
