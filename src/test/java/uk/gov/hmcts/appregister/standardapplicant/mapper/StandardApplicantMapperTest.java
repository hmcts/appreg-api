package uk.gov.hmcts.appregister.standardapplicant.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.projection.StandardApplicantEnrichedProjection;
import uk.gov.hmcts.appregister.data.StandardApplicantTestData;

class StandardApplicantMapperTest {
    private final StandardApplicantMapper mapper = new StandardApplicantMapperImpl();

    @Test
    void exposesOnlyReferenceFieldsDespitePopulatedPersonalDetails() throws Exception {
        val source = new StandardApplicantTestData().someComplete();
        val detail = mapper.toReadGetDto(source);
        val summary = mapper.toReadGetSummaryDto(projection(source));
        val print = mapper.toPrintRowDto(projection(source));

        assertProperties(detail, "code", "name", "startDate", "endDate");
        assertProperties(summary, "code", "name", "startDate", "endDate");
        assertProperties(print, "code", "name", "useFrom", "useTo");
        assertThat(detail.getCode()).isEqualTo(source.getApplicantCode());
        assertThat(detail.getName()).isEqualTo(source.getName());
        assertThat(detail.getStartDate()).isEqualTo(source.getApplicantStartDate());
        assertThat(detail.getEndDate().get()).isEqualTo(source.getApplicantEndDate());
        assertThat(summary.getName()).isEqualTo(source.getName());
        assertThat(summary.getEndDate().get()).isEqualTo(source.getApplicantEndDate());
        assertThat(print.getName().get()).isEqualTo(source.getName());
        assertThat(print.getCode().get()).isEqualTo(source.getApplicantCode());
        assertThat(print.getUseFrom().get()).isEqualTo(source.getApplicantStartDate());
        assertThat(print.getUseTo().get()).isEqualTo(source.getApplicantEndDate());
        val csv = mapper.toEntity(List.of(source)).getFirst();
        assertThat(csv.getApplicantCode()).isEqualTo(source.getApplicantCode());
        assertThat(csv.getName()).isEqualTo(source.getName());
        assertThat(csv.getApplicantStartDate())
                .isEqualTo(source.getApplicantStartDate().toString());
        assertThat(csv.getApplicantEndDate()).isEqualTo(source.getApplicantEndDate().toString());
    }

    @Test
    void doesNotDeriveMissingReferenceNameFromPersonalNames() {
        val source = new StandardApplicantTestData().someComplete();
        source.setName(null);
        source.setApplicantEndDate(null);
        assertThat(mapper.toReadGetDto(source).getName()).isNull();
        assertThat(mapper.toReadGetSummaryDto(projection(source)).getName()).isNull();
        assertThat(mapper.toReadGetDto(source).getEndDate().get()).isNull();
        assertThat(mapper.toPrintRowDto(projection(source)).getName().get()).isNull();
    }

    @Test
    void printIncludesExplicitNullsForMissingReferenceFields() {
        val print = mapper.toPrintRowDto(projection(new StandardApplicant()));
        assertThat(print.getCode().isPresent()).isTrue();
        assertThat(print.getCode().get()).isNull();
        assertThat(print.getName().isPresent()).isTrue();
        assertThat(print.getName().get()).isNull();
        assertThat(print.getUseFrom().isPresent()).isTrue();
        assertThat(print.getUseFrom().get()).isNull();
        assertThat(print.getUseTo().isPresent()).isTrue();
        assertThat(print.getUseTo().get()).isNull();
    }

    @Test
    void mapsSearchAuditFilters() {
        val from = LocalDate.of(2026, 1, 1);
        val to = LocalDate.of(2026, 12, 31);
        val entity =
                mapper.toEntity(
                        new CodeAndName("SA001", "Synthetic organisation", "excluded", from, to));
        assertThat(entity.getApplicantCode()).isEqualTo("SA001");
        assertThat(entity.getName()).isEqualTo("Synthetic organisation");
        assertThat(entity.getApplicantStartDate()).isEqualTo(from);
        assertThat(entity.getApplicantEndDate()).isEqualTo(to);
        assertThat(entity.getAddressLine1()).isEqualTo("excluded");
        assertThat(mapper.toEntity("SA001").getApplicantCode()).isEqualTo("SA001");
        assertThat(mapper.toReadGetDto(null)).isNull();
        assertThat(mapper.toReadGetSummaryDto(null)).isNull();
        assertThat(mapper.toPrintRowDto(null)).isNull();
    }

    private static StandardApplicantEnrichedProjection projection(StandardApplicant source) {
        return new StandardApplicantEnrichedProjection() {
            @Override
            public StandardApplicant getStandardApplicant() {
                return source;
            }

            @Override
            public String getEffectiveName() {
                return "Must not be used as a personal-name fallback";
            }
        };
    }

    private static void assertProperties(Object dto, String... fields) throws Exception {
        assertThat(
                        Arrays.stream(
                                        Introspector.getBeanInfo(dto.getClass(), Object.class)
                                                .getPropertyDescriptors())
                                .map(property -> property.getName()))
                .containsExactlyInAnyOrder(fields);
    }
}
