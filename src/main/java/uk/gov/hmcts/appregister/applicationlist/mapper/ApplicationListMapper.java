package uk.gov.hmcts.appregister.applicationlist.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.projection.ApplicationListSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = ApplicationListMappingHelper.class)
public interface ApplicationListMapper {

    @Mapping(target = "id", ignore = true)
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
    @Mapping(target = "entries", ignore = true)
    ApplicationList toCreateEntityWithCourt(ApplicationListCreateDto dto, NationalCourtHouse court);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "courtCode", ignore = true)
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "otherLocation", source = "dto.otherLocationDescription")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "time", source = "dto.time")
    @Mapping(target = "entries", ignore = true)
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
    @Mapping(target = "entriesSummary", source = "entriesSummary")
    ApplicationListGetDetailDto toGetDetailDto(
            ApplicationList appList,
            CriminalJusticeArea cja,
            long entryCount,
            List<ApplicationListEntrySummary> entriesSummary);

    @Mapping(target = "id", source = "appList.uuid")
    @Mapping(target = "date", source = "appList.date")
    @Mapping(target = "time", source = "appList.time")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "description", source = "appList.description")
    @Mapping(target = "entriesCount", source = "entryCount")
    @Mapping(target = "status", source = "appList.status")
    @Mapping(target = "entriesSummary", ignore = true)
    ApplicationListGetSummaryDto toGetSummaryDto(
            ApplicationListSummaryProjection appList, long entryCount, String location);

    @Mapping(target = "date", source = "appList.date")
    @Mapping(target = "time", source = "appList.time")
    @Mapping(target = "courtName", source = "appList.courtName")
    @Mapping(target = "otherLocationDescription", source = "appList.otherLocation")
    @Mapping(target = "duration", source = "appList", qualifiedByName = "formatDuration")
    @Mapping(target = "cja", source = "appList.cja", qualifiedByName = "formatCja")
    @Mapping(target = "entries", ignore = true)
    ApplicationListGetPrintDto toGetPrintDto(ApplicationList appList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "courtCode", expression = "java(null)")
    @Mapping(target = "courtName", expression = "java(null)")
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedDate", ignore = true)
    @Mapping(target = "otherLocation", source = "dto.otherLocationDescription")
    @Mapping(target = "durationMinutes", source = "dto.durationMinutes")
    @Mapping(target = "durationHours", source = "dto.durationHours")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "status", source = "dto.status.value")
    @Mapping(target = "time", source = "dto.time")
    @Mapping(target = "cja", source = "cja")
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "entries", ignore = true)
    void toUpdateEntityWithCja(
            ApplicationListUpdateDto dto,
            CriminalJusticeArea cja,
            @MappingTarget ApplicationList entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "otherLocation", ignore = true)
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedDate", ignore = true)
    @Mapping(target = "courtCode", source = "court.courtLocationCode")
    @Mapping(target = "courtName", source = "court.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "time", source = "dto.time")
    @Mapping(target = "status", source = "dto.status.value")
    @Mapping(target = "cja", source = "cja")
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "entries", ignore = true)
    void toUpdateEntityWithCourt(
            ApplicationListUpdateDto dto,
            CriminalJusticeArea cja,
            NationalCourtHouse court,
            @MappingTarget ApplicationList entity);
}
