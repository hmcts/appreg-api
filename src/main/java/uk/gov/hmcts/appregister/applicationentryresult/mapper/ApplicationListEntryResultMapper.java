package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListEntryResultMapper {

    @Autowired WordingTemplateMapper wordingTemplateMapper;

    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "entryId", source = "applicationList.uuid")
    @Mapping(target = "resultCode", source = "resolutionCode.resultCode")
    @Mapping(
            target = "wording",
            expression =
                    "java(wordingTemplateMapper.getTemplateDetail("
                            + "() -> appListEntryResolution.getResolutionCode().getWording(),"
                            + "() -> appListEntryResolution.getResolutionWording()))")
    public abstract ResultGetDto toResultGetDto(AppListEntryResolution appListEntryResolution);
}
