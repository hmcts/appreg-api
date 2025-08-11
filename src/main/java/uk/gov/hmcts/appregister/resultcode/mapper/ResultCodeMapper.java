package uk.gov.hmcts.appregister.resultcode.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.resultcode.dto.ResultCodeDto;
import uk.gov.hmcts.appregister.resultcode.model.ResultCode;

@Component
public class ResultCodeMapper {

    public ResultCodeDto toReadDto(ResultCode entity) {
        if (entity == null) {
            return null;
        }

        return new ResultCodeDto(
                entity.getId(),
                entity.getResultCode(),
                entity.getTitle(),
                entity.getWording(),
                entity.getLegislation(),
                entity.getDestinationEmail1(),
                entity.getDestinationEmail2(),
                entity.getStartDate(),
                entity.getEndDate());
    }

    public ResultCode toEntityFromReadDto(ResultCodeDto dto) {
        if (dto == null) {
            return null;
        }

        return ResultCode.builder()
                .id(dto.id())
                .resultCode(dto.resultCode())
                .title(dto.title())
                .wording(dto.wording())
                .legislation(dto.legislation())
                .destinationEmail1(dto.destinationEmail1())
                .destinationEmail2(dto.destinationEmail2())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .build();
    }
}
