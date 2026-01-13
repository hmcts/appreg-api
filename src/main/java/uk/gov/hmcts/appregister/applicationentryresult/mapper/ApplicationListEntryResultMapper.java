package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.template.wording.WordingTemplateSentence;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ApplicationListEntryResultMapper {

    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "entryId", source = "applicationList.uuid")
    @Mapping(target = "resultCode", source = "resolutionCode.resultCode")
    @Mapping(
        target = "wordingFields",
        expression = "java(getTemplateKeys(appListEntryResolution.getResolutionCode()))")
    public abstract ResultGetDto toResultGetDto(AppListEntryResolution appListEntryResolution);

    /**
     * gets the working reference strings from template code.
     *
     * @param code The resolution code
     * @return The list of template keys (references)
     */
    public List<String> getTemplateKeys(ResolutionCode code) {
        if (code == null || code.getWording() == null) {
            return List.of();
        }
        return WordingTemplateSentence.with(code.getWording()).getReferences();
    }
}
