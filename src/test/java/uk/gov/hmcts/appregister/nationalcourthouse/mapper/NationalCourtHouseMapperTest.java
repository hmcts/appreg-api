package uk.gov.hmcts.appregister.nationalcourthouse.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.common.entity.PettySessionalArea;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;

class NationalCourtHouseMapperTest {

    private final NationalCourtHouseMapper mapper = new NationalCourtHouseMapper();

    @Test
    void toReadDto_shouldReturnEmptyOptional_whenEntityIsNull() {
        // Act
        Optional<NationalCourtHouseDto> result = mapper.toReadDto(null);

        // Assert
        // When the input entity is null, the mapper should return Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void toReadDto_shouldMapEntityToDto_whenEntityIsValid() {
        // Arrange
        NationalCourtHouse entity =
                NationalCourtHouse.builder()
                        .id(1L)
                        .name("Cardiff Crown Court")
                        .courtType("CROWN")
                        .startDate(LocalDate.of(2020, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 31))
                        .locationId(100L)
                        .psaId(PettySessionalArea.builder().id(200L).build())
                        .courtLocationCode("CCC01")
                        .welshName("Llys y Goron Caerdydd")
                        .orgId(300L)
                        .build();

        // Act
        Optional<NationalCourtHouseDto> result = mapper.toReadDto(entity);

        // Assert
        // The mapper should produce a DTO with all the same field values as the entity
        assertThat(result).isPresent();
        NationalCourtHouseDto dto = result.get();
        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.name()).isEqualTo(entity.getName());
        assertThat(dto.courtType()).isEqualTo(entity.getCourtType());
        assertThat(dto.startDate()).isEqualTo(entity.getStartDate());
        assertThat(dto.endDate()).isEqualTo(entity.getEndDate());
        assertThat(dto.locationId()).isEqualTo(entity.getLocationId());
        assertThat(dto.psaId()).isEqualTo(entity.getPsaId().getId());
        assertThat(dto.courtLocationCode()).isEqualTo(entity.getCourtLocationCode());
        assertThat(dto.welshName()).isEqualTo(entity.getWelshName());
        assertThat(dto.orgId()).isEqualTo(entity.getOrgId());
    }

    @Test
    void toReadDto_shouldHandleEntityWithNullOptionalFields() {
        // Arrange
        NationalCourtHouse entity =
                NationalCourtHouse.builder()
                        .id(2L)
                        .name("Bristol Magistrates Court")
                        .courtType("MAGISTRATES")
                        .startDate(LocalDate.of(2019, 6, 1))
                        // End date and other fields left null intentionally
                        .build();

        // Act
        Optional<NationalCourtHouseDto> result = mapper.toReadDto(entity);

        // Assert
        // Null fields should remain null in the DTO
        assertThat(result).isPresent();
        NationalCourtHouseDto dto = result.get();
        assertThat(dto.endDate()).isNull();
        assertThat(dto.locationId()).isNull();
        assertThat(dto.psaId()).isNull();
        assertThat(dto.courtLocationCode()).isNull();
        assertThat(dto.welshName()).isNull();
        assertThat(dto.orgId()).isNull();
    }
}
