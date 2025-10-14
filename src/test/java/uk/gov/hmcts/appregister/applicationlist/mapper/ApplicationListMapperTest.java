package uk.gov.hmcts.appregister.applicationlist.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

/**
 * Unit tests for {@link ApplicationListMapper}. Uses MapStruct's Mappers.getMapper(...) to obtain
 * the generated implementation so we don't need a Spring context for these tests.
 */
public class ApplicationListMapperTest {

    private final ApplicationListMapper mapper = Mappers.getMapper(ApplicationListMapper.class);

    // ---------- Helper method tests ----------

    @Nested
    class ToMidnightTests {
        @Test
        void toMidnight_inputNull_returnNull() {
            assertNull(mapper.toMidnight(null));
        }

        @Test
        void toMidnight_validLocalDate_convertsToValidLocalDateTimeAtMidnight() {
            LocalDate d = LocalDate.of(2025, 9, 17);
            String expected = "2025-09-17T00:00";
            assertEquals(expected, mapper.toMidnight(d).toString());
        }
    }

    @Nested
    class CombineTests {
        @Test
        void combine_nullDateParam_returnsNull() {
            assertNull(mapper.combine(null, "10:30"));
        }

        @Test
        void combine_nullTimeParam_returnsNull() {
            assertNull(mapper.combine(LocalDate.of(2025, 9, 17), null));
        }

        @Test
        void combine_validDateAndTimeWithNoSecs_returnsValidLocalDateTime() {
            LocalDate date = LocalDate.of(2025, 9, 17);
            LocalDateTime ldt = mapper.combine(date, "10:30");
            assertEquals(LocalDateTime.of(2025, 9, 17, 10, 30, 0), ldt);
        }

        @Test
        void combine_validDateAndTimeWithSecs_returnsValidLocalDateTime() {
            LocalDate date = LocalDate.of(2025, 9, 17);
            LocalDateTime ldt = mapper.combine(date, "23:59:58");
            assertEquals(LocalDateTime.of(2025, 9, 17, 23, 59, 58), ldt);
        }
    }

    @Nested
    class ToTimeStringTests {
        @Test
        void returnsNullWhenInputIsNull() {
            assertNull(mapper.toTimeString(null));
        }

        @Test
        void emitsHHmmWhenSecondsAreZero() {
            LocalDateTime ldt = LocalDateTime.of(2025, 9, 17, 8, 5, 0);
            assertEquals("08:05", mapper.toTimeString(ldt));
        }

        @Test
        void emitsHHmmssWhenSecondsPresent() {
            LocalDateTime ldt = LocalDateTime.of(2025, 9, 17, 8, 5, 7);
            assertEquals("08:05:07", mapper.toTimeString(ldt));
        }
    }

    // ---------- Mapping: toCreateEntityWithCourt ----------

    @Nested
    class ToCreateEntityWithCourtTests {

        @Test
        void toCreateEntityWithCourt_validDtoWithCourt_returnsValidEntity() {

            ApplicationListCreateDto dto =
                    new ApplicationListCreateDto()
                            .date(LocalDate.of(2025, 9, 17))
                            .time("10:30")
                            .description("Morning session")
                            .status(ApplicationListStatus.OPEN)
                            .courtLocationCode("LOC123")
                            .durationHours(2)
                            .durationMinutes(45);

            NationalCourtHouse court =
                    NationalCourtHouse.builder()
                            .name("Bath Magistrates Court")
                            .courtLocationCode("LOC123")
                            .build();

            ApplicationList entity = mapper.toCreateEntityWithCourt(dto, court);

            assertNull(entity.getPk());
            assertNull(entity.getUuid());
            assertNull(entity.getVersion());
            assertNull(entity.getCreatedUser());

            assertEquals("LOC123", entity.getCourtCode());
            assertEquals("Bath Magistrates Court", entity.getCourtName());
            assertNull(entity.getCja());
            assertNull(entity.getOtherLocation());
            assertEquals("Morning session", entity.getDescription());
            assertEquals("OPEN", entity.getStatus());
            assertEquals(LocalDateTime.of(2025, 9, 17, 0, 0), entity.getDate());
            assertEquals(LocalDateTime.of(2025, 9, 17, 10, 30, 0), entity.getTime());
            assertEquals(2, entity.getDurationHours());
            assertEquals(45, entity.getDurationMinutes());
        }
    }

    @Nested
    class ToCreateEntityWithCjaTests {
        @Test
        void toCreateEntityWithCja_validDtoWithCja_returnsValidEntity() {

            ApplicationListCreateDto dto =
                    new ApplicationListCreateDto()
                            .date(LocalDate.of(2025, 9, 18))
                            .time("14:05:07")
                            .description("Afternoon session")
                            .otherLocationDescription("Temporary Courtroom at Town Hall")
                            .status(ApplicationListStatus.OPEN)
                            .durationHours(1)
                            .durationMinutes(15);

            CriminalJusticeArea cja =
                    CriminalJusticeArea.builder()
                            .id(123L)
                            .code("CJA001")
                            .description("Example CJA")
                            .build();

            ApplicationList entity = mapper.toCreateEntityWithCja(dto, cja);

            assertNull(entity.getPk());
            assertNull(entity.getUuid());
            assertNull(entity.getVersion());
            assertNull(entity.getCreatedUser());
            assertNull(entity.getCourtCode());
            assertNull(entity.getCourtName());

            assertNotNull(entity.getCja());
            assertEquals(123L, entity.getCja().getId());
            assertEquals("CJA001", entity.getCja().getCode());
            assertEquals("Temporary Courtroom at Town Hall", entity.getOtherLocation());
            assertEquals("Afternoon session", entity.getDescription());
            assertEquals("OPEN", entity.getStatus());
            assertEquals(LocalDateTime.of(2025, 9, 18, 0, 0), entity.getDate());
            assertEquals(LocalDateTime.of(2025, 9, 18, 14, 5, 7), entity.getTime());
            assertEquals(1, entity.getDurationHours());
            assertEquals(15, entity.getDurationMinutes());
        }
    }

    // ---------- Mapping: toGetDetailDto ----------

    @Nested
    class ToGetDetailDtoTests {

        @Test
        void toGetDetailDto_passedValidEntityWithNullCja_returnsValidDto() {
            UUID id = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            ApplicationList appList =
                    ApplicationList.builder()
                            .pk(999L)
                            .uuid(id)
                            .description("Morning session for traffic-related applications")
                            .status("OPEN")
                            .courtCode("LOC123")
                            .courtName("Bath Magistrates Court")
                            .date(LocalDateTime.of(2025, 9, 17, 0, 0))
                            .time(LocalDateTime.of(2025, 9, 17, 10, 30, 0))
                            .durationHours((short) 2)
                            .durationMinutes((short) 30)
                            .version(3L)
                            .build();

            // when
            ApplicationListGetDetailDto dto = mapper.toGetDetailDto(appList, null);

            assertNull(dto.getCjaCode());
            assertNull(dto.getOtherLocationDescription());

            assertEquals(id, dto.getId());
            assertEquals(LocalDate.of(2025, 9, 17), dto.getDate());
            assertEquals("10:30", dto.getTime());
            assertEquals("Morning session for traffic-related applications", dto.getDescription());
            assertEquals(ApplicationListStatus.OPEN, dto.getStatus());
            assertEquals("LOC123", dto.getCourtCode());
            assertEquals("Bath Magistrates Court", dto.getCourtName());
            assertEquals(2, dto.getDurationHours());
            assertEquals(30, dto.getDurationMinutes());
            assertEquals(3L, dto.getVersion());
        }
    }
}
