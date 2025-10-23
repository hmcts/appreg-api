package uk.gov.hmcts.appregister.resolutioncode.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeDto;
import uk.gov.hmcts.appregister.resolutioncode.dto.ResolutionCodeListItemDto;

class ResolutionCodeMapperTest {

    private ResolutionCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ResolutionCodeMapper();
    }

    // ---------- toReadDto ----------

    @Test
    void toReadDto_returnsEmpty_whenEntityIsNull() {
        Optional<ResolutionCodeDto> result = mapper.toReadDto(null);
        assertThat(result).isEmpty();
    }

    @Test
    void toReadDto_mapsAllFields() {
        ResolutionCode entity =
                ResolutionCode.builder()
                        .id(42L)
                        .resultCode("RC-001")
                        .title("Refused: Missing Info")
                        .wording("Application refused due to missing information.")
                        .legislation("Some Act 1998 s.10")
                        .destinationEmail1("primary@example.com")
                        .destinationEmail2("secondary@example.com")
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 1))
                        .build();

        ResolutionCodeDto dto = mapper.toReadDto(entity).orElseThrow();

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.resultCode()).isEqualTo("RC-001");
        assertThat(dto.title()).isEqualTo("Refused: Missing Info");
        assertThat(dto.wording()).isEqualTo("Application refused due to missing information.");
        assertThat(dto.legislation()).isEqualTo("Some Act 1998 s.10");
        assertThat(dto.destinationEmail1()).isEqualTo("primary@example.com");
        assertThat(dto.destinationEmail2()).isEqualTo("secondary@example.com");
        assertThat(dto.startDate())
                .isEqualTo(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(dto.endDate())
                .isEqualTo(OffsetDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void toReadDto_preservesNulls() {
        ResolutionCode entity =
                ResolutionCode.builder()
                        .id(7L)
                        .resultCode("RC-007")
                        .title("Conditional Approval")
                        .wording(null) // intentionally null
                        .legislation(null) // intentionally null
                        .destinationEmail1("owner@example.com")
                        .destinationEmail2(null) // intentionally null
                        .startDate(LocalDate.of(2024, 6, 1))
                        .endDate(null) // intentionally null
                        .build();

        ResolutionCodeDto dto = mapper.toReadDto(entity).orElseThrow();

        assertThat(dto.wording()).isNull();
        assertThat(dto.legislation()).isNull();
        assertThat(dto.destinationEmail2()).isNull();
        assertThat(dto.endDate()).isNull();
    }

    // ---------- toEntityFromReadDto ----------

    @Test
    void toEntityFromReadDto_returnsEmpty_whenDtoIsNull() {
        Optional<ResolutionCode> result = mapper.toEntityFromReadDto(null);
        assertThat(result).isEmpty();
    }

    @Test
    void toEntityFromReadDto_mapsAllFields() {
        ResolutionCodeDto dto =
                new ResolutionCodeDto(
                        100L,
                        "RC-100",
                        "Approved",
                        "Application approved.",
                        "Regulation 2020/1",
                        "approvals@example.com",
                        "audit@example.com",
                        LocalDate.of(2023, 5, 10),
                        LocalDate.of(2024, 5, 10));

        ResolutionCode entity = mapper.toEntityFromReadDto(dto).orElseThrow();

        assertThat(entity.getId()).isEqualTo(100L);
        assertThat(entity.getResultCode()).isEqualTo("RC-100");
        assertThat(entity.getTitle()).isEqualTo("Approved");
        assertThat(entity.getWording()).isEqualTo("Application approved.");
        assertThat(entity.getLegislation()).isEqualTo("Regulation 2020/1");
        assertThat(entity.getDestinationEmail1()).isEqualTo("approvals@example.com");
        assertThat(entity.getDestinationEmail2()).isEqualTo("audit@example.com");
        assertThat(entity.getStartDate())
                .isEqualTo(OffsetDateTime.of(2023, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(entity.getEndDate())
                .isEqualTo(OffsetDateTime.of(2024, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void toEntityFromReadDto_preservesNulls() {
        ResolutionCodeDto dto =
                new ResolutionCodeDto(
                        5L,
                        "RC-005",
                        "Pending",
                        null, // wording
                        null, // legislation
                        "queue@example.com",
                        null, // dest2
                        null, // startDate
                        null // endDate
                        );

        ResolutionCode entity = mapper.toEntityFromReadDto(dto).orElseThrow();

        assertThat(entity.getWording()).isNull();
        assertThat(entity.getLegislation()).isNull();
        assertThat(entity.getDestinationEmail2()).isNull();
        assertThat(entity.getStartDate()).isNull();
        assertThat(entity.getEndDate()).isNull();
    }

    // ---------- toListItem ----------

    @Test
    void toListItem_returnsEmpty_whenEntityIsNull() {
        Optional<ResolutionCodeListItemDto> result = mapper.toListItem(null);
        assertThat(result).isEmpty();
    }

    @Test
    void toListItem_mapsExpectedFields_onlyIdCodeTitle() {
        ResolutionCode entity =
                ResolutionCode.builder()
                        .id(9L)
                        .resultCode("RC-009")
                        .title("Rejected")
                        .wording("Some long wording that should NOT appear in list item")
                        .legislation("Irrelevant here")
                        .destinationEmail1("x@y.com")
                        .destinationEmail2("z@y.com")
                        .startDate(LocalDate.of(2022, 2, 2))
                        .endDate(LocalDate.of(2023, 3, 3))
                        .build();

        ResolutionCodeListItemDto item = mapper.toListItem(entity).orElseThrow();

        assertThat(item.id()).isEqualTo(9L);
        assertThat(item.code()).isEqualTo("RC-009");
        assertThat(item.title()).isEqualTo("Rejected");
        // compile-time guarantee: list item DTO exposes only id/resultCode/title
    }

    // ---------- Round-trip sanity ----------

    @Test
    void roundTrip_entity_toReadDto_backToEntity_preservesValues() {
        ResolutionCode original =
                ResolutionCode.builder()
                        .id(77L)
                        .resultCode("RC-077")
                        .title("Escalated")
                        .wording("Escalated for supervisor review.")
                        .legislation("Supervision Act 2012")
                        .destinationEmail1("supervisor@example.com")
                        .destinationEmail2(null)
                        .startDate(LocalDate.of(2024, 9, 1))
                        .endDate(null)
                        .build();

        ResolutionCodeDto dto = mapper.toReadDto(original).orElseThrow();
        ResolutionCode roundTripped = mapper.toEntityFromReadDto(dto).orElseThrow();

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getResultCode()).isEqualTo(original.getResultCode());
        assertThat(roundTripped.getTitle()).isEqualTo(original.getTitle());
        assertThat(roundTripped.getWording()).isEqualTo(original.getWording());
        assertThat(roundTripped.getLegislation()).isEqualTo(original.getLegislation());
        assertThat(roundTripped.getDestinationEmail1()).isEqualTo(original.getDestinationEmail1());
        assertThat(roundTripped.getDestinationEmail2()).isNull();
        assertThat(roundTripped.getStartDate()).isEqualTo(original.getStartDate());
        assertThat(roundTripped.getEndDate()).isNull();
    }
}
