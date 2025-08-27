package uk.gov.hmcts.appregister.courtlocation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourtLocationTest {

    @Test
    void noArgsConstructor_defaultsAreNull_andSettersUpdateValues() {
        // Create with no-args constructor (Lombok @NoArgsConstructor)
        CourtLocation cl = new CourtLocation();

        // All fields should default to null
        assertNull(cl.getId());
        assertNull(cl.getName());
        assertNull(cl.getCourtType());
        assertNull(cl.getStartDate());
        assertNull(cl.getEndDate());
        assertNull(cl.getLocationId());
        assertNull(cl.getPsaId());
        assertNull(cl.getCourtLocationCode());
        assertNull(cl.getWelshName());
        assertNull(cl.getOrgId());

        // Setting a couple of fields should be reflected by getters
        cl.setName("Cardiff Crown Court");
        cl.setCourtType("CROWN");
        assertEquals("Cardiff Crown Court", cl.getName());
        assertEquals("CROWN", cl.getCourtType());
    }

    @Test
    void settersThenGetters_roundTripAllFields() {
        // Prepare sample values to set on the entity
        Long id = 123L;
        String name = "Bristol Magistrates";
        String courtType = "MAGISTRATES";
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        Long locationId = 10L;
        Long psaId = 20L;
        String code = "BRIS01";
        String welsh = "—";
        Long orgId = 30L;

        CourtLocation cl = new CourtLocation();
        cl.setId(id);
        cl.setName(name);
        cl.setCourtType(courtType);
        cl.setStartDate(start);
        cl.setEndDate(end);
        cl.setLocationId(locationId);
        cl.setPsaId(psaId);
        cl.setCourtLocationCode(code);
        cl.setWelshName(welsh);
        cl.setOrgId(orgId);

        // Verify that getters return the same values that were set
        assertEquals(id, cl.getId());
        assertEquals(name, cl.getName());
        assertEquals(courtType, cl.getCourtType());
        assertEquals(start, cl.getStartDate());
        assertEquals(end, cl.getEndDate());
        assertEquals(locationId, cl.getLocationId());
        assertEquals(psaId, cl.getPsaId());
        assertEquals(code, cl.getCourtLocationCode());
        assertEquals(welsh, cl.getWelshName());
        assertEquals(orgId, cl.getOrgId());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        // Build values to pass to the all-args constructor (Lombok @AllArgsConstructor)
        Long id = 1L;
        String name = "Leeds Combined Court";
        String courtType = "CROWN";
        LocalDate start = LocalDate.of(2019, 7, 1);
        LocalDate end = LocalDate.of(2025, 7, 1);
        Long locationId = 100L;
        Long psaId = 200L;
        String code = "LEED01";
        String welsh = "Caerdydd"; // just a placeholder text
        Long orgId = 300L;

        CourtLocation cl = new CourtLocation(
            id,
            name,
            courtType,
            start,
            end,
            locationId,
            psaId,
            code,
            welsh,
            orgId
        );

        // Validate each field is set as provided
        assertEquals(id, cl.getId());
        assertEquals(name, cl.getName());
        assertEquals(courtType, cl.getCourtType());
        assertEquals(start, cl.getStartDate());
        assertEquals(end, cl.getEndDate());
        assertEquals(locationId, cl.getLocationId());
        assertEquals(psaId, cl.getPsaId());
        assertEquals(code, cl.getCourtLocationCode());
        assertEquals(welsh, cl.getWelshName());
        assertEquals(orgId, cl.getOrgId());
    }

    @Test
    void builder_setsAllFields() {
        // Use Lombok @Builder for a fluent construction
        CourtLocation cl = CourtLocation.builder()
            .id(42L)
            .name("Manchester Crown Court")
            .courtType("CROWN")
            .startDate(LocalDate.of(2018, 1, 1))
            .endDate(LocalDate.of(2026, 1, 1))
            .locationId(500L)
            .psaId(600L)
            .courtLocationCode("MAN01")
            .welshName("Manceinion")
            .orgId(700L)
            .build();

        // Validate every field was set through the builder
        assertEquals(42L, cl.getId());
        assertEquals("Manchester Crown Court", cl.getName());
        assertEquals("CROWN", cl.getCourtType());
        assertEquals(LocalDate.of(2018, 1, 1), cl.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 1), cl.getEndDate());
        assertEquals(500L, cl.getLocationId());
        assertEquals(600L, cl.getPsaId());
        assertEquals("MAN01", cl.getCourtLocationCode());
        assertEquals("Manceinion", cl.getWelshName());
        assertEquals(700L, cl.getOrgId());
    }

    @Test
    void equalsAndHashCode_respectAllFields() {
        // Two identical instances should be equal and share the same hashCode
        CourtLocation a = CourtLocation.builder()
            .id(1L)
            .name("Same")
            .courtType("CROWN")
            .startDate(LocalDate.of(2020, 1, 1))
            .endDate(null)
            .locationId(10L)
            .psaId(20L)
            .courtLocationCode("X")
            .welshName("W")
            .orgId(30L)
            .build();

        CourtLocation b = CourtLocation.builder()
            .id(1L)
            .name("Same")
            .courtType("CROWN")
            .startDate(LocalDate.of(2020, 1, 1))
            .endDate(null)
            .locationId(10L)
            .psaId(20L)
            .courtLocationCode("X")
            .welshName("W")
            .orgId(30L)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // Change one field to ensure inequality is detected
        CourtLocation c = CourtLocation.builder()
            .id(2L)
            .name("Same")
            .courtType("CROWN")
            .startDate(LocalDate.of(2020, 1, 1))
            .endDate(null)
            .locationId(10L)
            .psaId(20L)
            .courtLocationCode("X")
            .welshName("W")
            .orgId(30L)
            .build();

        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());
    }

    @Test
    void toString_includesClassNameAndKeyFields() {
        // Lombok @Data provides a toString implementation
        CourtLocation cl = CourtLocation.builder()
            .id(99L)
            .name("York")
            .courtType("MAGISTRATES")
            .startDate(LocalDate.of(2017, 6, 1))
            .endDate(null)
            .locationId(111L)
            .psaId(222L)
            .courtLocationCode("YRK1")
            .welshName("Llwybr")
            .orgId(333L)
            .build();

        String s = cl.toString();

        // Basic sanity checks on the output
        assertNotNull(s);
        assertTrue(s.contains("CourtLocation"));
        assertTrue(s.contains("id=99"));
        assertTrue(s.contains("name=York"));
        assertTrue(s.contains("courtType=MAGISTRATES"));
    }

    @Test
    void jpaAnnotations_presentWithExpectedValues() throws NoSuchFieldException {
        // Verify @Entity present
        Entity entityAnn = CourtLocation.class.getAnnotation(Entity.class);
        assertNotNull(entityAnn);

        // Verify @Table(name = "national_court_houses")
        Table tableAnn = CourtLocation.class.getAnnotation(Table.class);
        assertNotNull(tableAnn);
        assertEquals("national_court_houses", tableAnn.name());

        // Verify @Id, @GeneratedValue, and @Column on id field
        Field id = CourtLocation.class.getDeclaredField("id");
        assertNotNull(id.getAnnotation(Id.class));
        GeneratedValue gv = id.getAnnotation(GeneratedValue.class);
        assertNotNull(gv);
        assertEquals(GenerationType.IDENTITY, gv.strategy());
        Column idCol = id.getAnnotation(Column.class);
        assertNotNull(idCol);
        assertEquals("nch_id", idCol.name());
        assertFalse(idCol.nullable());
        assertFalse(idCol.updatable());

        // Verify selected @Column names on a few representative fields

        Field name = CourtLocation.class.getDeclaredField("name");
        Column nameCol = name.getAnnotation(Column.class);
        assertNotNull(nameCol);
        assertEquals("courthouse_name", nameCol.name());
        assertFalse(nameCol.nullable());

        Field courtType = CourtLocation.class.getDeclaredField("courtType");
        Column ctCol = courtType.getAnnotation(Column.class);
        assertNotNull(ctCol);
        assertEquals("court_type", ctCol.name());
        assertFalse(ctCol.nullable());

        Field startDate = CourtLocation.class.getDeclaredField("startDate");
        Column sdCol = startDate.getAnnotation(Column.class);
        assertNotNull(sdCol);
        assertEquals("start_date", sdCol.name());
        assertFalse(sdCol.nullable());

        Field endDate = CourtLocation.class.getDeclaredField("endDate");
        Column edCol = endDate.getAnnotation(Column.class);
        assertNotNull(edCol);
        assertEquals("end_date", edCol.name());

        Field courtLocationCode = CourtLocation.class.getDeclaredField("courtLocationCode");
        Column codeCol = courtLocationCode.getAnnotation(Column.class);
        assertNotNull(codeCol);
        assertEquals("court_location_code", codeCol.name());
    }
}
