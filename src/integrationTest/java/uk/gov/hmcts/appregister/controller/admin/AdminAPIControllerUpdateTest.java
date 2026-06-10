package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

class AdminAPIControllerUpdateTest extends AbstractAdminAPICrudTest {
    @Test
    void whenEnableDisableJobByName_thenReturnOk() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        assertEquals(200, responseSpec.getStatusCode());

        Response getResponseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName),
                        createAdminToken().fetchTokenForRole());

        var jobStatus = getResponseSpec.getBody().as(JobStatus.class);
        assertEquals(false, jobStatus.getEnabled());
        assertNull(jobStatus.getLastRan());

        restAssuredClient.executePutRequest(
                getLocalUrl(WEB_CONTEXT + "/" + jobName + "?enable=true"),
                createAdminToken().fetchTokenForRole(),
                null);
    }

    @Test
    void whenUpdateRetentionPeriodByName_thenReturnOk() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                WEB_CONTEXT
                                        + "/"
                                        + jobName
                                        + "/retention-policy?retentionPeriodDays=365"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        assertEquals(200, responseSpec.getStatusCode());
        assertEquals(
                "365",
                retentionPolicyRepository
                        .findByJobNameAndConfigKeyOrderByIdAsc(jobName, "RETENTION_PERIOD_DAYS")
                        .stream()
                        .findFirst()
                        .orElseThrow()
                        .getConfigValue());

        Response getResponseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName + "/retention-policy"),
                        createAdminToken().fetchTokenForRole());

        var retentionPolicy = getResponseSpec.getBody().as(JobRetentionPolicy.class);
        assertEquals(Integer.valueOf(365), retentionPolicy.getRetentionPeriodDays());

        restAssuredClient.executePutRequest(
                getLocalUrl(
                        WEB_CONTEXT + "/" + jobName + "/retention-policy?retentionPeriodDays=1825"),
                createAdminToken().fetchTokenForRole(),
                null);
    }

    @Test
    void whenEnableDisableJobByName_thenReturn404() throws Exception {
        var jobName = "SOME_JOB_THAT_DOES_NOT_EXIST";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        var problemDetail = responseSpec.getBody().as(ProblemDetail.class);
        assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getType().get(),
                problemDetail.getType());
        assertEquals(
                "Problem with value " + jobName + " for parameter jobType",
                problemDetail.getDetail());
    }

    @Test
    void whenUpdateRetentionPeriodByNameWithInvalidRetentionPeriod_thenReturn400()
            throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(
                                WEB_CONTEXT
                                        + "/"
                                        + jobName
                                        + "/retention-policy?retentionPeriodDays=0"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        assertEquals(400, responseSpec.getStatusCode());
        var responseBody = new ObjectMapper().readTree(responseSpec.asString());
        assertEquals(
                "updateDatabaseJobRetentionPeriodByName.retentionPeriodDays: must be greater than"
                        + " or equal to 1",
                responseBody.get("detail").asText());
    }
}
