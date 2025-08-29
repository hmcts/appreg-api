package uk.gov.hmcts.appregister.applicationresult.mapper;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultDto;
import uk.gov.hmcts.appregister.applicationresult.dto.ApplicationResultWriteDto;
import uk.gov.hmcts.appregister.applicationresult.model.ApplicationResult;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;
import uk.gov.hmcts.appregister.util.VersionManager;

@RequiredArgsConstructor
@Component
public class ApplicationResultMapper {

    private final ResolutionCodeMapper resultCodeMapper;
    private final VersionManager versionManager;

    public ApplicationResultDto toReadDto(ApplicationResult entity) {
        if (entity == null) {
            return null;
        }

        return new ApplicationResultDto(
                entity.getId(),
                resultCodeMapper.toReadDto(entity.getResultCode()),
                entity.getResultWording(),
                entity.getResultOfficer(),
                entity.getChangedBy(),
                entity.getChangedDate(),
                entity.getVersion());
    }

    public ApplicationResult createFromWriteDto(
            ApplicationResultWriteDto dto, String userId, LocalDate changedDate, String wording) {
        return ApplicationResult.builder()
                .resultWording(wording)
                .resultOfficer(dto.resultOfficer())
                .changedBy(userId)
                .changedDate(changedDate)
                .version(versionManager.increment(0))
                .build();
    }

    public void updateFromWriteDto(
            ApplicationResultWriteDto dto,
            ApplicationResult entity,
            String userId,
            LocalDate changedDate,
            String wording) {
        entity.setResultWording(wording);
        entity.setResultOfficer(dto.resultOfficer());
        entity.setChangedBy(userId);
        entity.setChangedDate(changedDate);
        entity.setVersion(versionManager.increment(0));
    }
}
