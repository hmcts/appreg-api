package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.mapper.OutgoingDtoSanitiser;
import uk.gov.hmcts.appregister.common.mapper.WordingTemplateMapper;
import uk.gov.hmcts.appregister.common.projection.ApplicationListEntryResultWithResultCodeProjection;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListEntryResultMapper {

    private WordingTemplateMapper wordingTemplateMapper;

    @Autowired
    public void setWordingTemplateMapper(WordingTemplateMapper wordingTemplateMapper) {
        this.wordingTemplateMapper = wordingTemplateMapper;
    }

    protected WordingTemplateMapper getWordingTemplateMapper() {
        return wordingTemplateMapper;
    }

    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "entryId", source = "applicationList.uuid")
    @Mapping(target = "resultCode", source = "resolutionCode.resultCode")
    @Mapping(target = "updatedDateTime", source = "changedDate")
    @Mapping(
            target = "wording",
            expression =
                    "java(getWordingTemplateMapper().getStoredTemplateDetail("
                            + "() -> appListEntryResolution.getResolutionCode().getWording(),"
                            + "() -> appListEntryResolution.getResolutionWording()))")
    public abstract ResultGetDto toResultGetDto(AppListEntryResolution appListEntryResolution);

    @Mapping(target = "id", source = "resolution.uuid")
    @Mapping(target = "entryId", source = "resolution.applicationList.uuid")
    @Mapping(target = "resultCode", source = "resolutionCode.resultCode")
    @Mapping(target = "updatedDateTime", source = "resolution.changedDate")
    @Mapping(
            target = "wording",
            expression =
                    "java(getWordingTemplateMapper().getStoredTemplateDetail("
                            + "() -> appListEntryResolution.getResolutionCode().getWording(),"
                            + "() -> appListEntryResolution.getResolution().getResolutionWording()))")
    public abstract ResultGetDto toResultGetDto(
            ApplicationListEntryResultWithResultCodeProjection appListEntryResolution);

    @AfterMapping
    protected void sanitizeDto(@MappingTarget ResultGetDto target) {
        OutgoingDtoSanitiser.sanitize(target);
    }
}
