package uk.gov.hmcts.appregister.mapper;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.dto.read.ApplicationListDto;
import uk.gov.hmcts.appregister.dto.write.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.model.ApplicationList;
import uk.gov.hmcts.appregister.model.CourtHouse;

@RequiredArgsConstructor
@Component
public class ApplicationListMapper {

    private final CourtHouseMapper courtHouseMapper;

    public ApplicationListDto toReadDto(ApplicationList entity) {
        if (entity == null) {
            return null;
        }

        return new ApplicationListDto(
                entity.getId(),
                entity.getStatus(),
                entity.getDate(),
                entity.getTime(),
                entity.getDescription(),
                courtHouseMapper.toReadDto(entity.getCourthouse()),
                entity.getChangedBy(),
                entity.getChangedDate(),
                entity.getVersion());
    }

    public ApplicationList createEntityFromWriteDto(
            ApplicationListWriteDto dto,
            String userId,
            LocalDate changedDate,
            CourtHouse courtHouse) {
        return ApplicationList.builder()
                .status(dto.status())
                .date(dto.date())
                .time(dto.time())
                .description(dto.description())
                .userId(userId)
                .changedBy(userId)
                .changedDate(changedDate)
                .courthouse(courtHouse)
                .build();
    }

    public void updateEntityFromWriteDto(
            ApplicationListWriteDto dto,
            ApplicationList entity,
            String userId,
            LocalDate changedDate,
            CourtHouse courtHouse) {
        entity.setStatus(dto.status());
        entity.setDate(dto.date());
        entity.setTime(dto.time());
        entity.setDescription(dto.description());
        entity.setChangedBy(userId);
        entity.setChangedDate(changedDate);
        entity.setCourthouse(courtHouse);
    }
}
