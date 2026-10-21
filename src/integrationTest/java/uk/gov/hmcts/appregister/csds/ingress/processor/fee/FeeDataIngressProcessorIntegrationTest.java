package uk.gov.hmcts.appregister.csds.ingress.processor.fee;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.fee.ingress-target=fee_staging",
            "appreg.csds.ingress.processors.fee.reporting-dir=${java.io.tmpdir}"
        })
class FeeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired private FeeDataIngressProcessor processor;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema:public}")
    private String schema;

    @Test
    void enrichesLegacyAndPreservesVersionsAcrossReplay() {
        jdbcTemplate.update(
                """
                INSERT INTO %s.fee_staging
                    (fee_id, fee_reference, fee_description, fee_value, fee_start_date,
                     fee_version, is_offsite, fee_changed_by, fee_changed_date, fee_user_name)
                VALUES (33, 'CO10.1', 'Original', 245, '2024-03-01', 1, false, 0, current_timestamp, 'test')
                """
                        .formatted(schema));
        var source =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CivilFeeID", 696)
                        .put("PSSFixedListID", 33)
                        .put("FeeReference", "CO10.1")
                        .put("Description", "Enriched")
                        .put("FeeValue", 300)
                        .put("StartDate", "2024-03-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", 2);
        var page = OBJECT_MAPPER.createObjectNode();
        page.putArray("records").add(source);
        processor.apply(processor.preProcess(List.of(page)));
        var legacy =
                jdbcTemplate.queryForMap(
                        "SELECT * FROM %s.fee_staging WHERE fee_id = 33".formatted(schema));
        assertThat(legacy.get("csds_id")).isEqualTo(696);

        source.put("CivilFeeID", 697).put("Description", "New version");
        processor.apply(processor.preProcess(List.of(page)));
        var version =
                jdbcTemplate.queryForMap(
                        "SELECT * FROM %s.fee_staging WHERE fee_id = 100697".formatted(schema));
        assertThat(version.get("csds_id")).isEqualTo(697);
        source.put("Description", "Ignored replay");
        processor.apply(processor.preProcess(List.of(page)));
        assertThat(
                        jdbcTemplate.queryForMap(
                                "SELECT * FROM %s.fee_staging WHERE fee_id = 100697"
                                        .formatted(schema)))
                .isEqualTo(version);

        source.put("CivilFeeID", 698).put("StartDate", "2025-01-01");
        processor.apply(processor.preProcess(List.of(page)));
        var laterPeriod =
                jdbcTemplate.queryForMap(
                        "SELECT * FROM %s.fee_staging WHERE fee_id = 100698".formatted(schema));
        assertThat(laterPeriod.get("csds_id")).isEqualTo(698);
        assertThat(laterPeriod.get("fee_start_date").toString()).startsWith("2025-01-01");
        assertThat(
                        jdbcTemplate.queryForMap(
                                "SELECT * FROM %s.fee_staging WHERE fee_id = 33".formatted(schema)))
                .isEqualTo(legacy);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM %s.fee_staging WHERE fee_reference = 'CO10.1'"
                                        .formatted(schema),
                                Integer.class))
                .isEqualTo(3);
    }
}
