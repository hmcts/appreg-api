package uk.gov.hmcts.appregister.applicationlist.mapper;

import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.projection.ApplicationListSummaryProjection;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListEntrySummary;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetPrintDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ApplicationListUpdateDto;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = ApplicationListMappingHelper.class)
public abstract class ApplicationListMapper {

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
    public abstract ApplicationList toCreateEntityWithCourt(
            ApplicationListCreateDto dto, NationalCourtHouse court);

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
    public abstract ApplicationList toCreateEntityWithCja(
            ApplicationListCreateDto dto, CriminalJusticeArea cja);

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
    public abstract ApplicationListGetDetailDto toGetDetailDto(
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
    public abstract ApplicationListGetSummaryDto toGetSummaryDto(
            ApplicationListSummaryProjection appList, long entryCount, String location);

    @Mapping(target = "date", source = "appList.date")
    @Mapping(target = "time", source = "appList.time")
    @Mapping(target = "courtName", source = "appList.courtName")
    @Mapping(target = "otherLocationDescription", source = "appList.otherLocation")
    @Mapping(target = "duration", source = "appList", qualifiedByName = "formatDuration")
    @Mapping(target = "cja", source = "appList.cja", qualifiedByName = "formatCja")
    @Mapping(target = "entries", ignore = true)
    public abstract ApplicationListGetPrintDto toGetPrintDto(ApplicationList appList);

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
    public abstract void toUpdateEntityWithCja(
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
    public abstract void toUpdateEntityWithCourt(
            ApplicationListUpdateDto dto,
            CriminalJusticeArea cja,
            NationalCourtHouse court,
            @MappingTarget ApplicationList entity);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "courtCode", ignore = true)
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "otherLocation", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "time", ignore = true)
    @Mapping(target = "entries", ignore = true)
    @Mapping(target = "cja", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "durationHours", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    public abstract ApplicationList toEntity(UUID id);

    @Mapping(target = "id", constant = "0L")
    @Mapping(target = "uuid", constant = "00000000-0000-0000-0000-000000000000")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(
            target = "courtCode",
            expression =
                    "java(applicationListGetFilterDto.getCourtLocationCode() != null ? "
                            + "applicationListGetFilterDto.getCourtLocationCode() : \"\")")
    @Mapping(
            target = "description",
            expression =
                    "java(applicationListGetFilterDto.getDescription() != null ? "
                            + "applicationListGetFilterDto.getDescription() : \"\")")
    @Mapping(target = "date", source = "applicationListGetFilterDto.date")
    @Mapping(target = "time", source = "applicationListGetFilterDto.time")
    @Mapping(
            target = "otherLocation",
            expression =
                    "java(applicationListGetFilterDto.getOtherLocationDescription() != null ? "
                            + "applicationListGetFilterDto.getOtherLocationDescription() : \"\")")
    @Mapping(
            target = "status",
            expression = "java(toStatus(applicationListGetFilterDto.getStatus()))")
    @Mapping(
            target = "cja.code",
            expression =
                    "java(applicationListGetFilterDto.getCjaCode() != null ? "
                            + "applicationListGetFilterDto.getCjaCode() : \"\")")
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "entries", ignore = true)
    @Mapping(target = "durationHours", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    public abstract ApplicationList toEntity(
            ApplicationListGetFilterDto applicationListGetFilterDto);

    /**
     * Convert the ApplicationListStatus enum from the generated model to the internal Status enum.
     * This method checks for null values.
     *
     * @param status The application list status to covert
     * @return The converted status
     */
    public static Status toStatus(ApplicationListStatus status) {
        Status retStatus = null;
        if (status != null) {
            retStatus = Status.valueOf(status.getValue());
        }
        return retStatus;
    }
}
