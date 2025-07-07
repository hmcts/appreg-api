package uk.gov.hmcts.appregister.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.read.CourtHouseDto;
import uk.gov.hmcts.appregister.model.CourtHouse;

@Component
public class CourtHouseMapper {

    public CourtHouseDto toReadDto(CourtHouse entity) {
        if (entity == null) return null;

        return new CourtHouseDto(
            entity.getId(),
            entity.getName(),
            entity.getCourtType(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getLocationId(),
            entity.getPsaId(),
            entity.getCourtLocationCode(),
            entity.getWelshName(),
            entity.getOrgId()
        );
    }
}

