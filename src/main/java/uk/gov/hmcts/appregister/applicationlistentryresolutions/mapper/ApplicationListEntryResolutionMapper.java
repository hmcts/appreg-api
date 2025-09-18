package uk.gov.hmcts.appregister.applicationlistentryresolutions.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionDto;
import uk.gov.hmcts.appregister.applicationlistentryresolutions.dto.ApplicationListEntryResolutionWriteDto;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.mapper.ResolutionCodeMapper;

@RequiredArgsConstructor
@Component
public class ApplicationListEntryResolutionMapper {

    private final ResolutionCodeMapper resolutionCodeMapper;

    public ApplicationListEntryResolutionDto toReadDto(AppListEntryResolution entity) {
        if (entity == null) {
            return null;
        }

        ResolutionCodeDto rcDto =
                resolutionCodeMapper.toReadDto(entity.getResolutionCode()).orElse(null);

        return new ApplicationListEntryResolutionDto(
                entity.getId(),
                rcDto,
                entity.getResolutionWording(),
                entity.getResolutionOfficer(),
                entity.getChangedBy().toString(),
                entity.getChangedDate(),
                entity.getVersion());
    }

    public AppListEntryResolution createFromWriteDto(
            ApplicationListEntryResolutionWriteDto dto, String wording) {
        return AppListEntryResolution.builder()
                .resolutionWording(wording)
                .resolutionOfficer(dto.resultOfficer())
                .build();
    }

    public void updateFromWriteDto(
            ApplicationListEntryResolutionWriteDto dto,
            AppListEntryResolution entity,
            String wording) {
        entity.setResolutionWording(wording);
        entity.setResolutionOfficer(dto.resultOfficer());
    }
}
