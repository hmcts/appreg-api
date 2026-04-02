package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.admin.exception.JobError;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

public class AdminAPIControllerUpdateTest extends AbstractAdminAPICrudTest {
    @Test
    public void whenEnableDisableJobByName_thenReturnOk() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + jobName + "/" + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        assertEquals(200, responseSpec.getStatusCode());

        Response getResponseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + jobName), createAdminToken().fetchTokenForRole());

        var jobStatus = getResponseSpec.getBody().as(JobStatus.class);
        assertEquals(false, jobStatus.getEnabled());
        assertNull(jobStatus.getLastRan());
    }

    @Test
    public void whenEnableDisableJobByName_thenReturn404() throws Exception {
        var jobName = "SOME_JOB_THAT_DOES_NOT_EXIST";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + jobName + "/" + "?enable=false"),
                        createAdminToken().fetchTokenForRole(),
                        null);

        var problemDetail = responseSpec.getBody().as(ProblemDetail.class);
        assertEquals(JobError.JOB_NOT_FOUND.getCode().getType().get(), problemDetail.getType());
    }

    @Test
    public void whenEnableDisableJobByName_userRole_thenReturn403() throws Exception {
        var jobName = "APPLICATION_LISTS_DATABASE_JOB";

        Response responseSpec =
                restAssuredClient.executePutRequest(
                        getLocalUrl(WEB_CONTEXT + jobName + "/" + "?enable=false"),
                        createUserToken().fetchTokenForRole(),
                        null);

        assertEquals(403, responseSpec.getStatusCode());
    }
}
