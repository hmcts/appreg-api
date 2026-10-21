package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.standard-applicants.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.standard-applicants.ingress-target=standard_applicants",
            "appreg.csds.ingress.processors.standard-applicants.primary-key=sa_id"
        })
class StandardApplicantDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired private StandardApplicantDataIngressProcessor processor;
    @Autowired private StandardApplicantRepository repository;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private BusinessDateProvider dates;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void preservesHistoryAcrossVersionsAndReplaysButEndDatesMissingCodes() {
        var legacy = repository.findAll().getFirst();
        var original = source(legacy.getId(), 1500000000L, legacy.getApplicantCode(), "Original");
        var first = processor.ingest(List.of(page(original)));
        assertThat(first.getUpdated()).isEqualTo(1);
        assertThat(first.getInserted()).isZero();
        var replacement =
                source(legacy.getId(), 1500000001L, legacy.getApplicantCode(), "Replacement");
        replacement.put("StartDate", "2021-01-01");
        var second = processor.ingest(List.of(page(replacement)));
        assertThat(second.getInserted()).isEqualTo(1);
        assertThat(second.getUpdated()).isZero();
        var params =
                new MapSqlParameterSource()
                        .addValue("legacyId", legacy.getId())
                        .addValue("newId", 1500100001L);
        var sql =
                "SELECT * FROM %s.standard_applicants WHERE sa_id IN (:legacyId, :newId) ORDER BY sa_id"
                        .formatted(schema);
        var saved = jdbc.queryForList(sql, params);
        assertThat(saved).hasSize(2);
        assertThat(((Number) saved.getFirst().get("csds_id")).longValue()).isEqualTo(1500000000L);
        assertThat(((Number) saved.getLast().get("csds_id")).longValue()).isEqualTo(1500000001L);
        saved.forEach(row -> assertThat(row.get("standard_applicant_end_date")).isNull());
        for (var record : List.of(original, replacement)) {
            var replay = processor.ingest(List.of(page(record)));
            assertThat(replay.getInserted()).isZero();
            assertThat(replay.getUpdated()).isZero();
        }
        assertThat(jdbc.queryForList(sql, params)).isEqualTo(saved);
        var future = source(legacy.getId(), 1500000002L, legacy.getApplicantCode(), "Future");
        future.put("StartDate", dates.currentUkDate().plusDays(1).toString());
        var rejected = processor.ingest(List.of(page(future)));
        assertThat(rejected.getInserted()).isZero();
        assertThat(rejected.getUpdated()).isZero();
        assertThat(jdbc.queryForList(sql, params)).isEqualTo(saved);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM %s.standard_applicants WHERE csds_id = 1500000002"
                                        .formatted(schema),
                                Map.of(),
                                Long.class))
                .isZero();
        processor.ingest(List.of(page()));
        jdbc.queryForList(sql, params)
                .forEach(
                        row ->
                                assertThat(row.get("standard_applicant_end_date"))
                                        .isEqualTo(Date.valueOf(dates.currentUkDate())));
    }

    private ObjectNode source(Long legacyId, long remoteId, String code, String name) {
        var node = MAPPER.createObjectNode();
        node.put("PSSApplicantID", legacyId);
        node.put("ApplicantID", remoteId);
        node.put("Code", code);
        node.put("OrganisationName", name);
        node.put("StartDate", "2020-01-01");
        node.putNull("EndDate");
        node.put("RevisionNumber", 1);
        node.putObject("Address").put("Line1", "1 Test Street");
        node.putObject("ContactInformation");
        return node;
    }

    private ObjectNode page(ObjectNode... records) {
        var page = MAPPER.createObjectNode();
        var items = page.putArray("records");
        for (var record : records) {
            items.add(record);
        }
        return page;
    }
}
