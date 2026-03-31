package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.admin.exception.DatabaseJobError;
import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

public class AdminAPIControllerReadTest extends AbstractAdminAPICrudTest {
    @Test
    public void whenGetDatabaseJobStatusByName_thenReturnOk() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName),
                        createAdminToken().fetchTokenForRole());

        responseSpec.then().statusCode(200);
        var jobStatus = responseSpec.getBody().as(DatabaseJobStatus.class);
        assertEquals(true, jobStatus.getEnabled());
        assertNull(jobStatus.getLastRan());
    }

    @Test
    public void whenGetDatabaseJobStatusByName_thenReturn404() throws Exception {
        var jobName = "SOME_JOB_THAT_DOES_NOT_EXIST";

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName),
                        createAdminToken().fetchTokenForRole());

        var problemDetail = responseSpec.getBody().as(ProblemDetail.class);
        assertEquals(
                DatabaseJobError.JOB_NOT_FOUND.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void whenGetDatabaseJobStatusByName_jobExists_userRole_thenReturn403() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + DATABASE_JOBS_PATH + jobName),
                        createUserToken().fetchTokenForRole());

        assertEquals(403, responseSpec.getStatusCode());
    }
}
