package uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.appregister.common.entity.DatabaseJob;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.CsdsIngressProcessor;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@TestPropertySource(
        properties = {
            "appreg.csds.ingress.page-size=1",
            "appreg.csds.ingress.processors.national-court-houses.enabled=true",
            "appreg.csds.ingress.processors.national-court-houses.reporting-dir=${java.io.tmpdir}",
            "appreg.csds.ingress.processors.national-court-houses.mock=",
            "appreg.csds.ingress.processors.national-court-houses.parameters=",
            "appreg.csds.ingress.processors.national-court-houses.backup-source=",
            "appreg.csds.ingress.processors.national-court-houses.backup-target=",
            "appreg.csds.ingress.processors.national-court-houses.ingress-target=national_court_houses_staging",
            "appreg.csds.ingress.processors.national-court-houses.primary-key=nch_id",
            "appreg.csds.ingress.base-url=${wiremock.server.baseUrl}",
            "appreg.csds.ingress.access-keys[0]=primary-test-key",
            "appreg.csds.ingress.access-keys[1]=secondary-test-key"
        })
class NationalCourtHouseDataIngressProcessorIntegrationTest extends BaseRepositoryTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private CsdsIngressProcessor csdsIngressProcessor;
    @Autowired private DatabaseJobRepository databaseJobRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @BeforeEach
    void setUpJobRowAndExistingCourtHouse() {
        var databaseJob = databaseJobRepository.findByName(CsdsIngressProcessor.DATABASE_JOB_NAME);
        if (databaseJob == null) {
            databaseJob =
                    databaseJobRepository.save(
                            DatabaseJob.builder()
                                    .name(CsdsIngressProcessor.DATABASE_JOB_NAME)
                                    .enabled(YesOrNo.YES)
                                    .build());
        }
        databaseJob.setEnabled(YesOrNo.YES);
        databaseJob.setMetadata(null);
        databaseJob.setLastRan(null);
        databaseJobRepository.save(databaseJob);

        jdbcTemplate.update(
                """
                INSERT INTO %s.national_court_houses_staging (
                    nch_id, courthouse_name, version_number, changed_by, changed_date,
                    court_type, start_date, court_location_code
                ) VALUES (?, ?, ?, ?, current_timestamp, ?, ?, ?)
                """
                        .formatted(schema),
                3106L,
                "Old Court",
                1L,
                0L,
                "CHOA",
                LocalDate.of(1900, 1, 1),
                "OLD");
    }

    @Test
    void given_nationalCourtHouseProcessorEnabled_when_runIngress_then_updatesAndInsertsRows()
            throws JsonProcessingException {
        var incomingRecords =
                List.of(
                        sourceRecord(3802L, 3106L, "Updated Court", null, "B01CF00", 2L),
                        sourceRecord(3803L, null, "New Court", "Llys Newydd", "B02CF00", 1L));

        stubFor(
                get(urlPathEqualTo("/count/CSDS/Court/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"count\":2}")));
        for (var offset = 0; offset < incomingRecords.size(); offset++) {
            var page = OBJECT_MAPPER.createObjectNode();
            page.putArray("records").add(incomingRecords.get(offset));
            stubFor(
                    get(urlEqualTo("/query/CSDS/Court/GD?%24limit=1&%24offset=" + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key"))
                            .willReturn(
                                    aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(OBJECT_MAPPER.writeValueAsString(page))));
        }

        assertThat(csdsIngressProcessor.runIngress()).isTrue();
        assertThat(loadCourtHouses())
                .containsExactly(
                        new StagedCourtHouse(3106L, "Updated Court", 2L, "CHOA", "B01CF00", null),
                        new StagedCourtHouse(
                                103803L, "New Court", 1L, "CHOA", "B02CF00", "Llys Newydd"));
        assertThat(
                        jdbcTemplate.queryForObject(
                                """
                                SELECT count(*)
                                FROM %s.national_court_houses_staging
                                WHERE loc_loc_id IS NOT NULL
                                   OR psa_psa_id IS NOT NULL
                                   OR norg_id IS NOT NULL
                                """
                                        .formatted(schema),
                                Integer.class))
                .isZero();

        verify(
                getRequestedFor(urlPathEqualTo("/count/CSDS/Court/GD"))
                        .withHeader("Api-Key", equalTo("primary-test-key")));
        for (var offset = 0; offset < incomingRecords.size(); offset++) {
            verify(
                    getRequestedFor(
                                    urlEqualTo(
                                            "/query/CSDS/Court/GD?%24limit=1&%24offset=" + offset))
                            .withHeader("Api-Key", equalTo("primary-test-key")));
        }
    }

    private List<StagedCourtHouse> loadCourtHouses() {
        return jdbcTemplate.query(
                """
                SELECT nch_id, courthouse_name, version_number, court_type,
                       court_location_code, sl_courthouse_name
                FROM %s.national_court_houses_staging
                ORDER BY nch_id
                """
                        .formatted(schema),
                (resultSet, rowNumber) ->
                        new StagedCourtHouse(
                                resultSet.getLong("nch_id"),
                                resultSet.getString("courthouse_name"),
                                resultSet.getLong("version_number"),
                                resultSet.getString("court_type"),
                                resultSet.getString("court_location_code"),
                                resultSet.getString("sl_courthouse_name")));
    }

    private ObjectNode sourceRecord(
            Long courtId,
            Long pssNationalCourtHouseId,
            String name,
            String welshName,
            String locationCode,
            Long revisionNumber) {
        var record =
                OBJECT_MAPPER
                        .createObjectNode()
                        .put("CourtID", courtId)
                        .put("CourtName", name)
                        .put("CourtLocationCode", locationCode)
                        .put("StartDate", "1900-01-01")
                        .putNull("EndDate")
                        .put("RevisionNumber", revisionNumber);
        record.put("PSSNationalCourtHouseID", pssNationalCourtHouseId);
        record.put("CourtWelshName", welshName);
        return record;
    }

    private record StagedCourtHouse(
            Long id,
            String name,
            Long version,
            String courtType,
            String locationCode,
            String welshName) {}
}
