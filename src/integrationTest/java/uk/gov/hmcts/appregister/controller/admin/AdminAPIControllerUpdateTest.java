package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.admin.exception.DatabaseJobError;
import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

public class AdminAPIControllerUpdateTest extends AbstractAdminAPICrudTest {
    @Test
    public void whenEnableDisableDatabaseJobByName_thenReturnOk() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        assertEquals(200, responseSpec.getStatusCode());

        Response getResponseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName),
                        createAdminToken().fetchTokenForRole());

        var jobStatus = getResponseSpec.getBody().as(DatabaseJobStatus.class);
        assertEquals(false, jobStatus.getEnabled());
        assertNull(jobStatus.getLastRan());
    }

    @Test
    public void whenEnableDisableDatabaseJobByName_thenReturn404() throws Exception {
        var jobName = "SOME_JOB_THAT_DOES_NOT_EXIST";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        var problemDetail = responseSpec.getBody().as(ProblemDetail.class);
        assertEquals(
                DatabaseJobError.JOB_NOT_FOUND.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void whenEnableDisableDatabaseJobByName_userRole_thenReturn403() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName + "?enable=false"),
                        createUserToken().fetchTokenForRole(),
                        null);

        assertEquals(403, responseSpec.getStatusCode());
    }
}
