package uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@Transactional
@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.resolution-codes.table-name=resolution_codes",
            "appreg.csds.ingress.processors.resolution-codes.primary-keys[0]=resolution_code",
            "appreg.csds.ingress.diagnostics.save-processed-json=false",
            "appreg.csds.ingress.diagnostics.save-diff-csv=false",
            "appreg.csds.ingress.diagnostics.log-diff-records=false"
        })
class ResolutionCodeDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    @Autowired private ResolutionCodeDataIngressProcessor processor;
    @Autowired private ResolutionCodeRepository repository;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager entityManager;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void existingCodePreservesIdReferencesAndAuditIdentityWithChangedOrMissingSourceIds() {
        var linkedId =
                jdbc.queryForObject(
                        "SELECT rc_rc_id FROM %s.app_list_entry_resolutions ORDER BY aler_id LIMIT 1"
                                .formatted(schema),
                        new MapSqlParameterSource(),
                        Long.class);
        var existing = repository.findById(linkedId).orElseThrow();
        var linkCount = linkCount(linkedId);
        assertThat(linkCount).isPositive();
        var source =
                source(existing)
                        .put("ResolutionCodeID", Long.MAX_VALUE)
                        .put("PSSResolutionCodeID", 999999L)
                        .put("ResultTitle", "Updated by code");
        assertThat(processor.ingest(List.of(page(source))).getUpdated()).isEqualTo(1);
        entityManager.clear();
        assertThat(repository.findById(linkedId).orElseThrow().getTitle())
                .isEqualTo("Updated by code");
        source.remove(List.of("ResolutionCodeID", "PSSResolutionCodeID"));
        source.put("ResultTitle", "Updated without IDs");
        assertThat(processor.ingest(List.of(page(source))).getUpdated()).isEqualTo(1);
        entityManager.clear();
        assertThat(repository.findById(linkedId).orElseThrow().getTitle())
                .isEqualTo("Updated without IDs");
        assertThat(linkCount(linkedId)).isEqualTo(linkCount);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM %s.csds_audit WHERE appreg_key = :id"
                                        .formatted(schema),
                                new MapSqlParameterSource("id", linkedId),
                                Long.class))
                .isEqualTo(2);
    }

    @Test
    void newCodesUsePssOrOffsetAndRepeatIngressRetainsId() {
        var source =
                source(repository.findAll().getFirst())
                        .put("Code", "ZZNEW")
                        .put("ResolutionCodeID", 900001L)
                        .putNull("PSSResolutionCodeID");
        assertThat(processor.ingest(List.of(page(source))).getInserted()).isEqualTo(1);
        assertThat(repository.findById(1000001L)).isPresent();
        source.put("ResolutionCodeID", 900002L).put("PSSResolutionCodeID", 800001L);
        assertThat(processor.ingest(List.of(page(source))).getUpdated()).isEqualTo(1);
        assertThat(repository.findById(800001L)).isEmpty();
        source.put("Code", "ZZPSS");
        assertThat(processor.ingest(List.of(page(source))).getInserted()).isEqualTo(1);
        assertThat(repository.findById(800001L)).isPresent();
    }

    @Test
    void duplicatesAndIdCollisionsFailWithoutApplyingEarlierValidRecords() {
        var existing = repository.findAll().getFirst();
        var first =
                source(existing)
                        .put("Code", "ZZNEW")
                        .put("ResolutionCodeID", 900001L)
                        .putNull("PSSResolutionCodeID");
        var duplicate = first.deepCopy().put("ResolutionCodeID", 900002L);
        var count = repository.count();
        assertThatThrownBy(() -> processor.ingest(List.of(page(first), page(duplicate))))
                .hasMessageContaining("Duplicate incoming resolution_code");
        var collision =
                first.deepCopy()
                        .put("Code", "ZZOTHER")
                        .put("PSSResolutionCodeID", existing.getId());
        assertThatThrownBy(() -> processor.ingest(List.of(page(first), page(collision))))
                .hasMessageContaining("Conflicting RC_ID");
        assertThat(repository.count()).isEqualTo(count);
        assertThat(repository.findById(1000001L)).isEmpty();
    }

    private long linkCount(Long id) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM %s.app_list_entry_resolutions WHERE rc_rc_id = :id"
                        .formatted(schema),
                new MapSqlParameterSource("id", id),
                Long.class);
    }

    private JsonNode page(JsonNode source) {
        return mapper.createObjectNode().set("records", mapper.createArrayNode().add(source));
    }

    private ObjectNode source(ResolutionCode code) {
        return mapper.createObjectNode()
                .put("Code", code.getResultCode())
                .put("ResolutionCodeID", code.getId())
                .putNull("PSSResolutionCodeID")
                .put("ResultTitle", code.getTitle())
                .put("ResultWording", code.getWording())
                .putNull("Legislation")
                .putNull("Recipient1Email")
                .putNull("Recipient2Email")
                .put("StartDate", "2020-01-01")
                .putNull("EndDate")
                .put("RevisionNumber", 1L);
    }
}
