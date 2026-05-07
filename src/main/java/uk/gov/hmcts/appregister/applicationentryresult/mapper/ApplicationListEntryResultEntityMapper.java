package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

/**
 * Maps result create DTOs to AppListEntryResolution entity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@Setter
public abstract class ApplicationListEntryResultEntityMapper {

    @Mapping(target = "applicationList", source = "applicationListEntry")
    @Mapping(target = "resolutionWording", source = "substituteWording")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    public abstract AppListEntryResolution toApplicationListEntryResult(
            ResultCreateDto resultCreateDto,
            String substituteWording,
            ResolutionCode resolutionCode,
            ApplicationListEntry applicationListEntry,
            String resolutionOfficer);

    @Mapping(target = "applicationList", source = "applicationListEntry")
    @Mapping(target = "resolutionWording", source = "substituteWording")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedDate", ignore = true)
    public abstract void toApplicationListEntryResult(
            ResultUpdateDto resultUpdateDto,
            String substituteWording,
            ResolutionCode resolutionCode,
            ApplicationListEntry applicationListEntry,
            String resolutionOfficer,
            @MappingTarget AppListEntryResolution entryResolution);

    @Mapping(target = "applicationList.uuid", source = "entryId")
    @Mapping(target = "applicationList.applicationList.uuid", source = "listId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedDate", ignore = true)
    @Mapping(target = "resolutionWording", ignore = true)
    @Mapping(target = "resolutionCode", ignore = true)
    @Mapping(target = "resolutionOfficer", ignore = true)
    public abstract AppListEntryResolution toApplicationListEntryResult(
            PayloadGetEntryResultInList entity,
            @MappingTarget AppListEntryResolution entryResolution);
}
