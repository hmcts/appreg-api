package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.resolution-codes.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.resolution-codes.ingress-target=resolution_codes",
            "appreg.csds.ingress.processors.resolution-codes.primary-key=rc_id"
        })
class ResolutionCodeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired private ResolutionCodeDataIngressProcessor processor;
    @Autowired private ResolutionCodeRepository repository;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void preservesLegacyAndModifiedVersionsAndSkipsReplay() {
        var legacy = repository.findAll().getFirst();
        var original = source(legacy.getId(), 1500000000L, legacy.getResultCode(), "Original");
        var first = processor.ingest(List.of(page(original)));
        assertThat(first.getUpdated()).isEqualTo(1);
        assertThat(first.getInserted()).isZero();
        var modified = source(legacy.getId(), 1500000001L, legacy.getResultCode(), "Modified");
        modified.put("EndDate", "2020-02-01");
        var second = processor.ingest(List.of(page(modified)));
        assertThat(second.getInserted()).isEqualTo(1);
        assertThat(second.getUpdated()).isZero();
        var params =
                new MapSqlParameterSource()
                        .addValue("legacyId", legacy.getId())
                        .addValue("newId", 1500100001L);
        var sql =
                "SELECT * FROM %s.resolution_codes WHERE rc_id IN (:legacyId, :newId) ORDER BY rc_id"
                        .formatted(schema);
        var saved = jdbc.queryForList(sql, params);
        assertThat(saved).hasSize(2);
        assertThat(((Number) saved.getFirst().get("csds_id")).longValue()).isEqualTo(1500000000L);
        assertThat(((Number) saved.getLast().get("csds_id")).longValue()).isEqualTo(1500000001L);
        for (var record : List.of(original, modified)) {
            var replay = processor.ingest(List.of(page(record)));
            assertThat(replay.getInserted()).isZero();
            assertThat(replay.getUpdated()).isZero();
        }
        assertThat(jdbc.queryForList(sql, params)).isEqualTo(saved);
    }

    private ObjectNode source(Long legacyId, long remoteId, String code, String title) {
        var node = MAPPER.createObjectNode();
        node.put("PSSResolutionCodeID", legacyId);
        node.put("ResolutionCodeID", remoteId);
        node.put("Code", code);
        node.put("ResultTitle", title);
        node.put("ResultWording", "Wording");
        node.putNull("Legislation");
        node.putNull("Recipient1Email");
        node.putNull("Recipient2Email");
        node.put("StartDate", "2020-01-01");
        node.putNull("EndDate");
        node.put("RevisionNumber", 1);
        return node;
    }

    private ObjectNode page(ObjectNode record) {
        var page = MAPPER.createObjectNode();
        page.putArray("records").add(record);
        return page;
    }
}
