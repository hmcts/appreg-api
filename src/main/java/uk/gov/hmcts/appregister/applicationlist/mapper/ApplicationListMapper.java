package uk.gov.hmcts.appregister.applicationlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ApplicationListMapper {

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "cja", ignore = true)
    @Mapping(target = "otherLocation", ignore = true)
    @Mapping(target = "courtCode", source = "court.courtLocationCode")
    @Mapping(target = "courtName", source = "court.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "time", source = "dto.time")
    ApplicationList toCreateEntityWithCourt(ApplicationListCreateDto dto, NationalCourtHouse court);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "courtCode", ignore = true)
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "otherLocation", source = "dto.otherLocationDescription")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "time", source = "dto.time")
    ApplicationList toCreateEntityWithCja(ApplicationListCreateDto dto, CriminalJusticeArea cja);

    @Mapping(target = "id", source = "appList.uuid")
    @Mapping(target = "date", source = "appList.date")
    @Mapping(target = "time", source = "appList.time")
    @Mapping(target = "description", source = "appList.description")
    @Mapping(target = "status", source = "appList.status")
    @Mapping(target = "cjaCode", expression = "java(cja != null ? cja.getCode() : null)")
    @Mapping(target = "courtCode", source = "appList.courtCode")
    @Mapping(target = "courtName", source = "appList.courtName")
    @Mapping(target = "otherLocationDescription", source = "appList.otherLocation")
    @Mapping(target = "durationHours", source = "appList.durationHours")
    @Mapping(target = "durationMinutes", source = "appList.durationMinutes")
    @Mapping(target = "version", source = "appList.version")
    @Mapping(target = "entriesCount", source = "entryCount")
    @Mapping(target = "entriesSummary", ignore = true)
    ApplicationListGetDetailDto toGetDetailDto(
            ApplicationList appList, CriminalJusticeArea cja, long entryCount);

    @Mapping(target = "id", source = "appList.uuid")
    @Mapping(target = "date", source = "appList.date")
    @Mapping(target = "time", source = "appList.time")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "description", source = "appList.description")
    @Mapping(target = "entriesCount", source = "entryCount")
    @Mapping(target = "status", source = "appList.status")
    @Mapping(target = "entriesSummary", ignore = true)
    ApplicationListGetSummaryDto toGetSummaryDto(
            ApplicationList appList, long entryCount, String location);
}
