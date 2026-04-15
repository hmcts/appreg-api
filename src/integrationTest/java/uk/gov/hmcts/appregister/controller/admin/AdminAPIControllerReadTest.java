package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

public class AdminAPIControllerReadTest extends AbstractAdminAPICrudTest {
    @Test
    public void whenGetJobStatusByName_thenReturnOk() throws Exception {
        var jobName = AdminJobType.APPLICATION_LISTS_DATABASE_JOB.name();

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName),
                        createAdminToken().fetchTokenForRole());

        responseSpec.then().statusCode(200);
        var jobStatus = responseSpec.getBody().as(JobStatus.class);
        assertEquals(true, jobStatus.getEnabled());
        assertNull(jobStatus.getLastRan());
    }

    @Test
    public void whenGetJobStatusByName_thenReturn404() throws Exception {
        var jobName = "SOME_JOB_THAT_DOES_NOT_EXIST";

        Response responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName),
                        createAdminToken().fetchTokenForRole());

        var problemDetail = responseSpec.getBody().as(ProblemDetail.class);
        assertEquals(
                CommonAppError.TYPE_MISMATCH_ERROR.getCode().getType().get(),
                problemDetail.getType());
        assertEquals(
                problemDetail.getDetail(),
                "Problem with value " + jobName + " for parameter jobType");
    }
}
