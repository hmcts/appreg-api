package uk.gov.hmcts.appregister.courtlocation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CourtHouseTest {

    @Test
    void settersThenGetters_returnAssignedValues() {
        Long id = 123L;
        String name = "Cardiff Crown Court";
        String courtType = "CROWN";
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Long locationId = 77L;
        Long psaId = 88L;
        String courtLocationCode = "1234";
        String welshName = "Llys y Goron Caerdydd";
        Long orgId = 999L;

        CourtHouse ch = new CourtHouse();
        ch.setId(id);
        ch.setName(name);
        ch.setCourtType(courtType);
        ch.setStartDate(startDate);
        ch.setEndDate(endDate);
        ch.setLocationId(locationId);
        ch.setPsaId(psaId);
        ch.setCourtLocationCode(courtLocationCode);
        ch.setWelshName(welshName);
        ch.setOrgId(orgId);

        assertEquals(id, ch.getId());
        assertEquals(name, ch.getName());
        assertEquals(courtType, ch.getCourtType());
        assertEquals(startDate, ch.getStartDate());
        assertEquals(endDate, ch.getEndDate());
        assertEquals(locationId, ch.getLocationId());
        assertEquals(psaId, ch.getPsaId());
        assertEquals(courtLocationCode, ch.getCourtLocationCode());
        assertEquals(welshName, ch.getWelshName());
        assertEquals(orgId, ch.getOrgId());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        Long id = 1L;
        String name = "Bristol Magistrates";
        String courtType = "MAGISTRATES";
        LocalDate startDate = LocalDate.of(2021, 2, 2);
        LocalDate endDate = LocalDate.of(2023, 3, 3);
        Long locationId = 10L;
        Long psaId = 20L;
        String courtLocationCode = "5678";
        String welshName = "—";
        Long orgId = 30L;

        CourtHouse ch =
                new CourtHouse(
                        id,
                        name,
                        courtType,
                        startDate,
                        endDate,
                        locationId,
                        psaId,
                        courtLocationCode,
                        welshName,
                        orgId);

        assertEquals(id, ch.getId());
        assertEquals(name, ch.getName());
        assertEquals(courtType, ch.getCourtType());
        assertEquals(startDate, ch.getStartDate());
        assertEquals(endDate, ch.getEndDate());
        assertEquals(locationId, ch.getLocationId());
        assertEquals(psaId, ch.getPsaId());
        assertEquals(courtLocationCode, ch.getCourtLocationCode());
        assertEquals(welshName, ch.getWelshName());
        assertEquals(orgId, ch.getOrgId());
    }

    @Test
    void builder_setsAllFields() {
        Long id = 42L;
        String name = "Manchester";
        String courtType = "CROWN";
        LocalDate startDate = LocalDate.of(2019, 7, 1);
        LocalDate endDate = LocalDate.of(2025, 8, 1);
        Long locationId = 100L;
        Long psaId = 200L;
        String courtLocationCode = "9012";
        String welshName = "Manceinion";
        Long orgId = 300L;

        CourtHouse ch =
                CourtHouse.builder()
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

        assertEquals(id, ch.getId());
        assertEquals(name, ch.getName());
        assertEquals(courtType, ch.getCourtType());
        assertEquals(startDate, ch.getStartDate());
        assertEquals(endDate, ch.getEndDate());
        assertEquals(locationId, ch.getLocationId());
        assertEquals(psaId, ch.getPsaId());
        assertEquals(courtLocationCode, ch.getCourtLocationCode());
        assertEquals(welshName, ch.getWelshName());
        assertEquals(orgId, ch.getOrgId());
    }

    @Test
    void noArgsConstructor_hasNullDefaults_andSettersUpdate() {
        CourtHouse ch = new CourtHouse();
        assertNull(ch.getId());
        assertNull(ch.getName());
        assertNull(ch.getCourtType());
        assertNull(ch.getStartDate());
        assertNull(ch.getEndDate());
        assertNull(ch.getLocationId());
        assertNull(ch.getPsaId());
        assertNull(ch.getCourtLocationCode());
        assertNull(ch.getWelshName());
        assertNull(ch.getOrgId());

        ch.setName("Leeds");
        assertEquals("Leeds", ch.getName());
    }
}
