package uk.gov.hmcts.appregister.nationalcourthouse.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.nationalcourthouse.dto.NationalCourtHouseDto;
import uk.gov.hmcts.appregister.nationalcourthouse.model.NationalCourtHouse;

class NationalCourtHouseMapperTest {

    private final NationalCourtHouseMapper mapper = new NationalCourtHouseMapper();

    @Test
    void toReadDto_whenEntityIsNull_returnsNull() {
        // Arrange
        NationalCourtHouse entity = null;

        // Act
        NationalCourtHouseDto dto = mapper.toReadDto(entity);

        // Assert
        // Defensive behaviour: null in -> null out (no exception)
        assertThat(dto).isNull();
    }

    @Test
    void toReadDto_mapsAllFields_fromEntityToDto() {
        // Arrange: build a fully-populated entity
        Long id = 42L;
        String name = "Cardiff Crown Court";
        String courtType = "CROWN";
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Long locationId = 77L;
        Long psaId = 88L;
        String courtLocationCode = "1234";
        String welshName = "Llys y Goron Caerdydd";
        Long orgId = 999L;

        NationalCourtHouse entity =
                NationalCourtHouse.builder()
                        .id(id)
                        .name(name)
                        .courtType(courtType)
                        .startDate(startDate)
                        .endDate(endDate)
                        .locationId(locationId)
                        .psaId(psaId)
                        .courtLocationCode(courtLocationCode)
                        .welshName(welshName)
                        .orgId(orgId)
                        .build();

        // Act
        NationalCourtHouseDto dto = mapper.toReadDto(entity);

        // Assert: field-for-field mapping is preserved
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.courtType()).isEqualTo(courtType);
        assertThat(dto.startDate()).isEqualTo(startDate);
        assertThat(dto.endDate()).isEqualTo(endDate);
        assertThat(dto.locationId()).isEqualTo(locationId);
        assertThat(dto.psaId()).isEqualTo(psaId);
        assertThat(dto.courtLocationCode()).isEqualTo(courtLocationCode);
        assertThat(dto.welshName()).isEqualTo(welshName);
        assertThat(dto.orgId()).isEqualTo(orgId);
    }

    @Test
    void toReadDto_whenOptionalFieldsAreNull_propagatesNulls() {
        // Arrange: entity with nullable/optional fields left null
        // (endDate and welshName are typical nullables)
        NationalCourtHouse entity =
                NationalCourtHouse.builder()
                        .id(7L)
                        .name("Bristol Magistrates")
                        .courtType("MAGISTRATES")
                        .startDate(LocalDate.of(2021, 5, 10))
                        .endDate(null) // intentionally null
                        .locationId(10L)
                        .psaId(20L)
                        .courtLocationCode("5678")
                        .welshName(null) // intentionally null
                        .orgId(30L)
                        .build();

        // Act
        NationalCourtHouseDto dto = mapper.toReadDto(entity);

        // Assert: nulls are passed through as-is, no surprises
        assertThat(dto).isNotNull();
        assertThat(dto.endDate()).isNull();
        assertThat(dto.welshName()).isNull();

        // And non-null values are still mapped
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Bristol Magistrates");
        assertThat(dto.courtType()).isEqualTo("MAGISTRATES");
        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2021, 5, 10));
        assertThat(dto.locationId()).isEqualTo(10L);
        assertThat(dto.psaId()).isEqualTo(20L);
        assertThat(dto.courtLocationCode()).isEqualTo("5678");
        assertThat(dto.orgId()).isEqualTo(30L);
    }
}
