package uk.gov.hmcts.appregister.applicationlist.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ApplicationListMapper {

    // ---- helpers ----
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    /**
     * Convert a {@link LocalDate} into a {@link LocalDateTime} at midnight.
     *
     * <p>This is used for persisting the "date" column in the entity, where the database column
     * type is TIMESTAMP but only the calendar date is relevant. The time component is set to
     * 00:00:00.
     *
     * @param date the {@code LocalDate} from the DTO (yyyy-MM-dd)
     * @return a {@code LocalDateTime} at start of day, or {@code null} if input is null
     */
    default LocalDateTime toMidnight(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    /**
     * Combine a {@link LocalDate} and a time string into a {@link LocalDateTime}.
     *
     * <p>The DTO represents date and time separately: date as {@code LocalDate}, and time as a
     * validated string in "HH:mm" or "HH:mm:ss" format. Because the database columns are TIMESTAMP,
     * we must merge these into a full {@code LocalDateTime} before persisting.
     *
     * @param date the calendar date (yyyy-MM-dd) from the DTO
     * @param time the time string ("HH:mm" or "HH:mm:ss") from the DTO
     * @return a combined {@code LocalDateTime}, or {@code null} if either part is null
     */
    default LocalDateTime combine(LocalDate date, String time) {
        if (date == null || time == null) {
            return null;
        }
        LocalTime lt = LocalTime.parse(time, TIME_FORMAT);
        return date.atTime(lt);
    }

    /**
     * Extract a time-of-day string from a {@link LocalDateTime}.
     *
     * <p>This is used when mapping back from the entity to the DTO. Only the {@code LocalTime} part
     * is relevant, formatted in ISO-8601 style ("HH:mm" or "HH:mm:ss").
     *
     * @param ldt the {@code LocalDateTime} from the entity
     * @return a time string suitable for the DTO, or {@code null} if input is null
     */
    default String toTimeString(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        LocalTime t = ldt.toLocalTime();
        // emit seconds only if present
        return t.getSecond() == 0
                ? t.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString()
                : t.toString(); // ISO "HH:mm:ss"
    }

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "cja", ignore = true)
    @Mapping(target = "otherLocation", ignore = true)
    @Mapping(target = "courtCode", source = "court.courtLocationCode")
    @Mapping(target = "courtName", source = "court.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", expression = "java(toMidnight(dto.getDate()))")
    @Mapping(target = "time", expression = "java(combine(dto.getDate(), dto.getTime()))")
    ApplicationList toCreateEntityWithCourt(ApplicationListCreateDto dto, NationalCourtHouse court);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdUser", ignore = true)
    @Mapping(target = "courtCode", ignore = true)
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "otherLocation", source = "dto.otherLocationDescription")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "date", expression = "java(toMidnight(dto.getDate()))")
    @Mapping(target = "time", expression = "java(combine(dto.getDate(), dto.getTime()))")
    ApplicationList toCreateEntityWithCja(ApplicationListCreateDto dto, CriminalJusticeArea cja);

    @Mapping(target = "id", source = "appList.uuid")
    @Mapping(target = "date", expression = "java(appList.getDate().toLocalDate())")
    @Mapping(target = "time", expression = "java(toTimeString(appList.getTime()))")
    @Mapping(target = "description", source = "appList.description")
    @Mapping(target = "status", source = "appList.status")
    @Mapping(target = "cjaCode", expression = "java(cja != null ? cja.getCode() : null)")
    @Mapping(target = "courtCode", source = "appList.courtCode")
    @Mapping(target = "courtName", source = "appList.courtName")
    @Mapping(target = "otherLocationDescription", source = "appList.otherLocation")
    @Mapping(target = "durationHours", source = "appList.durationHours")
    @Mapping(target = "durationMinutes", source = "appList.durationMinutes")
    @Mapping(target = "version", source = "appList.version")
    ApplicationListGetDetailDto toGetDetailDto(ApplicationList appList, CriminalJusticeArea cja);

    @Mapping(target = "id", source = "appList.uuid")
    @Mapping(target = "date", expression = "java(appList.getDate().toLocalDate())")
    @Mapping(target = "time", expression = "java(toTimeString(appList.getTime()))")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "description", source = "appList.description")
    @Mapping(target = "numberOfEntries", source = "entryCount")
    @Mapping(target = "status", source = "appList.status")
    ApplicationListGetSummaryDto toGetSummaryDto(
        ApplicationList appList,
        long entryCount,
        String location);
}
