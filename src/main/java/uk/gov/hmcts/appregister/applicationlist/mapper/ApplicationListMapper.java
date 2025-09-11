package uk.gov.hmcts.appregister.applicationlist.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListDto;
import uk.gov.hmcts.appregister.applicationlist.dto.ApplicationListWriteDto;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.mapper.NationalCourtHouseMapper;

/** Mapper for ApplicationList entity and its DTOs. */
@RequiredArgsConstructor
@Component
public class ApplicationListMapper {

    private final NationalCourtHouseMapper courtHouseMapper;

    @SuppressWarnings("java:S1135")
    public ApplicationListDto toReadDto(ApplicationList entity) {
        if (entity == null) {
            return null;
        }

        /* var courthouseDto =
                        java.util.Optional.ofNullable(entity.getCourthouse())
                                .flatMap(courtHouseMapper::toReadDto) // Optional<NationalCourtHouseDto>
                                .orElse(null); // <-- unwrap or null

        */
        // TODO: This will likely derive from a foreign key relation which does not currently exist
        NationalCourtHouseDto courtHouseDto =
                new NationalCourtHouseDto(
                        null,
                        entity.getCourthouseName(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        return new ApplicationListDto(
                entity.getId(),
                entity.getStatus(),
                entity.getDate(),
                entity.getTime().toString(),
                entity.getDescription(),
                courtHouseDto, // NationalCourtHouseDto (possibly null)
                entity.getChangedBy().toString(),
                entity.getChangedDate(),
                entity.getVersion().intValue());
    }

    public ApplicationList createEntityFromWriteDto(
            ApplicationListWriteDto dto, NationalCourtHouse courtHouse) {
        return ApplicationList.builder()
                .status(dto.status())
                .date(dto.date())
                .time(dto.time())
                .description(dto.description())
                .courthouseName(courtHouse.getName())
                .build();
    }

    public void updateEntityFromWriteDto(
            ApplicationListWriteDto dto, ApplicationList entity, NationalCourtHouse courtHouse) {
        entity.setStatus(dto.status());
        entity.setDate(dto.date());
        entity.setTime(dto.time());
        entity.setDescription(dto.description());
        entity.setCourthouseName(courtHouse.getName());
    }
}
