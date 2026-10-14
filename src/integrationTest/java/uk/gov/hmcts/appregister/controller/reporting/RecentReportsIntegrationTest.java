package uk.gov.hmcts.appregister.controller.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class RecentReportsIntegrationTest extends BaseIntegration {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserProvider userProvider;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @BeforeEach
    void authenticate() throws Exception {
        var authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal())
                .thenReturn(TokenGenerator.builder().build().getJwtFromToken());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void givenMixedJobs_whenListing_thenReturnsLatestSevenOwnedDownloadableReports()
            throws Exception {
        var owner = userProvider.getUserId();
        var expected = new ArrayList<String>();
        var types =
                List.of(
                        JobType.ACTIVITY_AUDIT_REPORT,
                        JobType.FEES_REPORT,
                        JobType.LIST_MAINTENANCE_REPORT,
                        JobType.SEARCH_WARRANTS_REPORT,
                        JobType.WORKLOAD_REPORT,
                        JobType.DURATION_REPORT,
                        JobType.PRIVATE_PROSECUTORS_INDEX_REPORT);
        for (int i = 0; i < 10; i++) {
            expected.addFirst(
                    insert(owner, types.get(i % types.size()), "COMPLETED", "Report\nCSV\n", i)
                            .toString());
        }
        insert("another-user", JobType.FEES_REPORT, "COMPLETED", "CSV", 20);
        insert(owner, JobType.BULK_UPLOAD_ENTRIES, "COMPLETED", "CSV", 20);
        for (var state : List.of("SUBMITTED", "PENDING", "RUNNING", "FAILED")) {
            insert(owner, JobType.FEES_REPORT, state, "CSV", 20);
        }
        insert(owner, JobType.FEES_REPORT, "COMPLETED", null, 20);
        insert(owner, JobType.FEES_REPORT, "COMPLETED", "", 20);

        var token = getToken();
        var response = restAssuredClient.executeGetRequest(getLocalUrl("reports/jobs"), token);
        response.then().statusCode(200);
        assertEquals("no-store", response.header("Cache-Control"));
        assertEquals(expected.subList(0, 7), response.jsonPath().getList("id", String.class));
        assertEquals(7L, response.jsonPath().getList("type").stream().distinct().count());
        var originalTimestamp = response.jsonPath().getString("[0].completedAt");
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + schema + ".asynch_jobs", Long.class);
        for (int i = 0; i < 2; i++) {
            var download =
                    restAssuredClient.executeGetRequest(
                            getLocalUrl("reports/jobs/" + expected.getFirst() + "/download"),
                            token);
            download.then().statusCode(200);
            assertEquals("Report\nCSV\n", download.asString());
        }
        assertEquals(
                count,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + schema + ".asynch_jobs", Long.class));
        var refreshed = restAssuredClient.executeGetRequest(getLocalUrl("reports/jobs"), token);
        assertEquals(originalTimestamp, refreshed.jsonPath().getString("[0].completedAt"));

        var otherToken =
                getATokenWithValidCredentials()
                        .oid(UUID.randomUUID().toString())
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole();
        var otherReports =
                restAssuredClient.executeGetRequest(getLocalUrl("reports/jobs"), otherToken);
        otherReports.then().statusCode(200);
        assertEquals(List.of(), otherReports.jsonPath().getList(""));
        restAssuredClient
                .executeGetRequest(
                        getLocalUrl("reports/jobs/" + expected.getFirst() + "/download"),
                        otherToken)
                .then()
                .statusCode(404);
    }

    @Test
    void givenMatchingCompletionTimes_whenListing_thenUsesJobSequenceForStableOrder()
            throws Exception {
        var first = insert(userProvider.getUserId(), JobType.FEES_REPORT, "COMPLETED", "CSV", 0);
        var second =
                insert(userProvider.getUserId(), JobType.DURATION_REPORT, "COMPLETED", "CSV", 0);
        var response = restAssuredClient.executeGetRequest(getLocalUrl("reports/jobs"), getToken());
        response.then().statusCode(200);
        assertEquals(
                List.of(second.toString(), first.toString()),
                response.jsonPath().getList("id", String.class));
    }

    private UUID insert(String owner, JobType type, String state, String csv, int minute) {
        var id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO "
                        + schema
                        + ".asynch_jobs "
                        + "(id, user_name, job_type, job_state, csv_output, last_updated) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                owner,
                type.getValue(),
                state,
                csv,
                Timestamp.valueOf(LocalDateTime.of(2026, 10, 7, 12, minute)));
        return id;
    }
}
