package uk.gov.hmcts.appregister.applicationentryresult.mapper;

import lombok.Setter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;

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
    //@Mapping(target = "resolutionOfficer", ignore = true)
    @Mapping(target = "resolutionOfficer", constant = "SYSTEM")
    @Mapping(target = "createdUser", ignore = true)
    public abstract AppListEntryResolution toApplicationListEntryResult(
        ResultCreateDto resultCreateDto,
        String substituteWording,
        //String resolutionOfficer,
        ResolutionCode resolutionCode,
        ApplicationListEntry applicationListEntry//,
        //String createdUser
    );
}
