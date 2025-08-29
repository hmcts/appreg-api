package uk.gov.hmcts.appregister.resolutioncode.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ResolutionCodeTest {

    @Test
    void settersThenGetters_returnAssignedValues() {
        // Arrange: make a blank entity and set every field via setters
        ResolutionCode rc = new ResolutionCode();
        Long id = 10L;
        String code = "RC001";
        String title = "Case Dismissed";
        String wording = "The case is dismissed.";
        String legislation = "Some Act 1999 s.1";
        String email1 = "primary@hmcts.net";
        String email2 = "secondary@hmcts.net";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        rc.setId(id);
        rc.setResultCode(code);
        rc.setTitle(title);
        rc.setWording(wording);
        rc.setLegislation(legislation);
        rc.setDestinationEmail1(email1);
        rc.setDestinationEmail2(email2);
        rc.setStartDate(start);
        rc.setEndDate(end);

        // Assert: getters return what we set
        assertThat(rc.getId()).isEqualTo(id);
        assertThat(rc.getResultCode()).isEqualTo(code);
        assertThat(rc.getTitle()).isEqualTo(title);
        assertThat(rc.getWording()).isEqualTo(wording);
        assertThat(rc.getLegislation()).isEqualTo(legislation);
        assertThat(rc.getDestinationEmail1()).isEqualTo(email1);
        assertThat(rc.getDestinationEmail2()).isEqualTo(email2);
        assertThat(rc.getStartDate()).isEqualTo(start);
        assertThat(rc.getEndDate()).isEqualTo(end);
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        // Arrange + Act: construct using generated all-args constructor
        ResolutionCode rc =
                new ResolutionCode(
                        7L,
                        "RC777",
                        "Conviction Recorded",
                        "A conviction is recorded.",
                        "CJA 2003",
                        "dest1@hmcts.net",
                        "dest2@hmcts.net",
                        LocalDate.of(2020, 2, 2),
                        LocalDate.of(2021, 3, 3));

        // Assert: each field was set as expected
        assertThat(rc.getId()).isEqualTo(7L);
        assertThat(rc.getResultCode()).isEqualTo("RC777");
        assertThat(rc.getTitle()).isEqualTo("Conviction Recorded");
        assertThat(rc.getWording()).isEqualTo("A conviction is recorded.");
        assertThat(rc.getLegislation()).isEqualTo("CJA 2003");
        assertThat(rc.getDestinationEmail1()).isEqualTo("dest1@hmcts.net");
        assertThat(rc.getDestinationEmail2()).isEqualTo("dest2@hmcts.net");
        assertThat(rc.getStartDate()).isEqualTo(LocalDate.of(2020, 2, 2));
        assertThat(rc.getEndDate()).isEqualTo(LocalDate.of(2021, 3, 3));
    }

    @Test
    void builder_setsAllFields() {
        // Act: use Lombok builder for a fluent construction
        ResolutionCode rc =
                ResolutionCode.builder()
                        .id(42L)
                        .resultCode("RC042")
                        .title("Appeal Successful")
                        .wording("The appeal is successful.")
                        .legislation("Appeals Act 2010")
                        .destinationEmail1("a@hmcts.net")
                        .destinationEmail2("b@hmcts.net")
                        .startDate(LocalDate.of(2022, 5, 1))
                        .endDate(null) // open-ended
                        .build();

        // Assert: builder populated all fields
        assertThat(rc.getId()).isEqualTo(42L);
        assertThat(rc.getResultCode()).isEqualTo("RC042");
        assertThat(rc.getTitle()).isEqualTo("Appeal Successful");
        assertThat(rc.getWording()).isEqualTo("The appeal is successful.");
        assertThat(rc.getLegislation()).isEqualTo("Appeals Act 2010");
        assertThat(rc.getDestinationEmail1()).isEqualTo("a@hmcts.net");
        assertThat(rc.getDestinationEmail2()).isEqualTo("b@hmcts.net");
        assertThat(rc.getStartDate()).isEqualTo(LocalDate.of(2022, 5, 1));
        assertNull(rc.getEndDate());
    }

    @Test
    void noArgsConstructor_defaultsAreNull_andSettersWork() {
        // Arrange: no-args constructor should produce a blank object
        ResolutionCode rc = new ResolutionCode();

        // Assert: Lombok @Data default values are null
        assertNull(rc.getId());
        assertNull(rc.getResultCode());
        assertNull(rc.getTitle());
        assertNull(rc.getWording());
        assertNull(rc.getLegislation());
        assertNull(rc.getDestinationEmail1());
        assertNull(rc.getDestinationEmail2());
        assertNull(rc.getStartDate());
        assertNull(rc.getEndDate());

        // And verify setters change state
        rc.setResultCode("RCX");
        assertThat(rc.getResultCode()).isEqualTo("RCX");
    }

    @Test
    void toString_includesKeyFields() {
        // Arrange
        ResolutionCode rc =
                ResolutionCode.builder()
                        .id(5L)
                        .resultCode("RC005")
                        .title("Some Title")
                        .startDate(LocalDate.of(2024, 1, 1))
                        .build();

        // Act
        String s = rc.toString();

        // Assert: Lombok @Data toString should include field names/values
        assertThat(s).contains("id=5");
        assertThat(s).contains("resultCode=RC005");
        assertThat(s).contains("title=Some Title");
    }

    // ---- Light-touch JPA mapping checks via reflection ----

    @Test
    void jpaAnnotations_presentOnClassAndFields() throws Exception {
        // Entity + Table present with expected name
        assertThat(ResolutionCode.class.isAnnotationPresent(Entity.class)).isTrue();

        Table table = ResolutionCode.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("resolution_codes");

        // @Id present on 'id' with expected @Column(name)
        Field id = ResolutionCode.class.getDeclaredField("id");
        assertThat(id.isAnnotationPresent(Id.class)).isTrue();
        Column idCol = id.getAnnotation(Column.class);
        assertThat(idCol).isNotNull();
        assertThat(idCol.name()).isEqualTo("rc_id");
        assertThat(idCol.nullable()).isFalse();
        assertThat(idCol.updatable()).isFalse();

        // Spot-check a few other columns
        assertColumn("resultCode", "resolution_code", true, 10);
        assertColumn("title", "resolution_code_title", true, 500);
        assertColumn("wording", "resolution_code_wording", true, -1);
        assertColumn("legislation", "resolution_legislation", true, -1);
        assertColumn("destinationEmail1", "rc_destination_email_address_1", true, -1);
        assertColumn("destinationEmail2", "rc_destination_email_address_2", true, -1);
        assertColumn("startDate", "resolution_code_start_date", true, -1);
        assertColumn("endDate", "resolution_code_end_date", true, -1);
    }

    private static void assertColumn(
            String fieldName,
            String expectedColName,
            boolean expectPresent,
            int expectedLengthOrMinus1)
            throws NoSuchFieldException {
        Field f = ResolutionCode.class.getDeclaredField(fieldName);
        Column col = f.getAnnotation(Column.class);
        if (expectPresent) {
            assertThat((Annotation) col).as("Column annotation on %s", fieldName).isNotNull();
            assertThat(col.name()).isEqualTo(expectedColName);
            if (expectedLengthOrMinus1 > 0) {
                assertThat(col.length()).isEqualTo(expectedLengthOrMinus1);
            }
        } else {
            assertThat(col).isNull();
        }
    }
}
