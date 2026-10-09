package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.processors.standard-applicants.table-name=standard_applicants",
            "appreg.csds.ingress.processors.standard-applicants.primary-keys[0]=standard_applicant_code",
            "appreg.csds.ingress.diagnostics.save-processed-json=false",
            "appreg.csds.ingress.diagnostics.save-incoming-csv=false",
            "appreg.csds.ingress.diagnostics.save-existing-csv=false",
            "appreg.csds.ingress.diagnostics.save-diff-csv=false",
            "appreg.csds.ingress.diagnostics.log-diff-records=false"
        })
class StandardApplicantDataIngressProcessorIntegrationTest extends BaseRepositoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired private StandardApplicantDataIngressProcessor processor;
    @Autowired private StandardApplicantRepository applicants;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDateProvider businessDateProvider;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    @Transactional
    void existingApplicantRetainsIdReferenceAndAuditIdentityAcrossChangedAndMissingSourceIds() {
        jdbc.update(
                ("UPDATE %s.configuration_parameters SET parameter_value='DEBUG' "
                                + "WHERE parameter_name='AUDIT_CSDS'")
                        .formatted(schema));
        var storedId =
                jdbc.queryForObject(
                        "SELECT sa_sa_id FROM %s.application_list_entries WHERE sa_sa_id IS NOT NULL LIMIT 1"
                                .formatted(schema),
                        Long.class);
        var stored = applicants.findById(storedId).orElseThrow();
        var linkCount = linkCount(storedId);
        assertThat(linkCount).isPositive();
        final var count = applicants.count();

        var changed = source(stored.getApplicantCode(), 777L, 888L, "Updated organisation");
        processor.ingest(List.of(page(changed)));
        entityManager.clear();

        assertThat(applicants.count()).isEqualTo(count);
        assertThat(applicants.findById(storedId).orElseThrow().getName())
                .isEqualTo("Updated organisation");
        assertThat(linkCount(storedId)).isEqualTo(linkCount);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM %s.csds_audit WHERE appreg_key=?"
                                        .formatted(schema),
                                Long.class,
                                storedId))
                .isPositive();
        var auditedSource =
                jdbc.queryForObject(
                        "SELECT csds_json FROM %s.csds_audit WHERE appreg_key=? ORDER BY ca_id DESC LIMIT 1"
                                .formatted(schema),
                        String.class,
                        storedId);
        assertThat(auditedSource).contains("Updated organisation").contains("777").contains("888");

        changed.remove(List.of("ApplicantID", "PSSApplicantID"));
        processor.ingest(List.of(page(changed)));
        entityManager.clear();
        assertThat(applicants.count()).isEqualTo(count);
        assertThat(applicants.findById(storedId).orElseThrow().getName())
                .isEqualTo("Updated organisation");
        assertThat(linkCount(storedId)).isEqualTo(linkCount);
    }

    @Test
    @Transactional
    void newApplicantsUsePssOrOffsetAndRepeatedIngressDoesNotAddRows() {
        final var count = applicants.count();
        var pss = source("CSDSPS", 333L, 990001L, "PSS applicant");
        var offset = source("CSDSOFF", 890002L, null, "Offset applicant");
        processor.ingest(List.of(page(pss), page(offset)));
        processor.ingest(List.of(page(pss), page(offset)));
        entityManager.clear();

        assertThat(applicants.count()).isEqualTo(count + 2);
        assertThat(applicants.findById(990001L).orElseThrow().getApplicantCode())
                .isEqualTo("CSDSPS");
        assertThat(applicants.findById(990002L).orElseThrow().getApplicantCode())
                .isEqualTo("CSDSOFF");
        var applicant = applicants.findById(990002L).orElseThrow();
        assertThat(applicant.getEmailAddress()).isEqualTo("email@example.test");
        assertThat(applicant.getTelephoneNumber()).isEqualTo("020 1234 5678");
        assertThat(applicant.getAddressLine1()).isEqualTo("<missing>");
        assertThat(applicant.getApplicantEndDate()).isNull();
    }

    @Test
    void invalidBatchesDoNotUpdateEndDatesOrInsertRows() {
        var before =
                jdbc.queryForList(
                        "SELECT * FROM %s.standard_applicants ORDER BY sa_id".formatted(schema));
        var existingId = applicants.findStandardApplicantByCode("APP001").getFirst().getId();
        var valid = source("CSDSNEW", 890003L, null, "New applicant");
        var duplicate = source("CSDSNEW", 890004L, null, "Duplicate");
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(duplicate))))
                .hasMessageContaining("Duplicate incoming standard_applicant_code");
        var collision = source("CSDSBAD", 1L, existingId, "Collision");
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(collision))))
                .hasMessageContaining("Conflicting SA_ID");
        var noId = source("CSDSBAD", null, null, "Missing source ID");
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(noId))))
                .hasMessageContaining("Missing source ID");
        var invalidDate =
                source("CSDSBAD", null, null, "Invalid future date")
                        .put("StartDate", "2099-01-01")
                        .put("EndDate", "invalid");
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(invalidDate))))
                .isInstanceOf(RuntimeException.class);
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM %s.standard_applicants ORDER BY sa_id"
                                        .formatted(schema)))
                .isEqualTo(before);
    }

    @Test
    void failedUpsertRollsBackMissingApplicantEndDatingAndAnyInserts() {
        var before =
                jdbc.queryForList(
                        "SELECT * FROM %s.standard_applicants ORDER BY sa_id".formatted(schema));
        var valid = source("CSDSNEW", 890003L, null, "New applicant");
        var invalid = source("CSDSBAD", 890004L, null, "x".repeat(500));
        assertThatThrownBy(() -> processor.ingest(List.of(page(valid), page(invalid))))
                .isInstanceOf(RuntimeException.class);
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM %s.standard_applicants ORDER BY sa_id"
                                        .formatted(schema)))
                .isEqualTo(before);
    }

    @Test
    @Transactional
    void droppedFutureApplicantIsMissingForExistingReconciliationPolicy() {
        var stored = applicants.findStandardApplicantByCode("APP001").getFirst();
        var activeIds =
                applicants.findAll().stream()
                        .filter(item -> item.getApplicantEndDate() == null)
                        .map(StandardApplicant::getId)
                        .toList();
        assertThat(activeIds).contains(stored.getId());
        final var count = applicants.count();
        var future =
                source(stored.getApplicantCode(), null, null, "Future version")
                        .put("StartDate", "2099-01-01");
        processor.ingest(List.of(page(future)));
        entityManager.clear();

        assertThat(applicants.count()).isEqualTo(count);
        for (var id : activeIds) {
            assertThat(applicants.findById(id).orElseThrow().getApplicantEndDate())
                    .isEqualTo(businessDateProvider.currentUkDate());
        }
        assertThat(applicants.findById(stored.getId()).orElseThrow().getName())
                .isEqualTo(stored.getName());
    }

    private ObjectNode source(String code, Long sourceId, Long pssId, String name) {
        var node = MAPPER.createObjectNode();
        node.put("Code", code)
                .put("OrganisationName", name)
                .put("StartDate", "2020-01-01")
                .putNull("EndDate")
                .put("RevisionNumber", 1L);
        if (sourceId != null) {
            node.put("ApplicantID", sourceId);
        }
        if (pssId != null) {
            node.put("PSSApplicantID", pssId);
        }
        node.putArray("Address");
        var contacts = node.putArray("ContactInformation");
        contacts.addObject()
                .put("ContactType", "Email Address")
                .put("ContactValue", "email@example.test");
        contacts.addObject().put("ContactType", "Telephone").put("ContactValue", "020 1234 5678");
        return node;
    }

    private long linkCount(Long id) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM %s.application_list_entries WHERE sa_sa_id=?"
                        .formatted(schema),
                Long.class,
                id);
    }

    private ObjectNode page(ObjectNode... records) {
        var node = MAPPER.createObjectNode();
        var array = node.putArray("records");
        for (var record : records) {
            array.add(record);
        }
        return node;
    }
}
