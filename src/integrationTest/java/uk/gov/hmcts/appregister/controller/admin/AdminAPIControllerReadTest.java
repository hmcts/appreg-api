package uk.gov.hmcts.appregister.controller.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.restassured.response.Response;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.admin.audit.AdminAuditOperation;
import uk.gov.hmcts.appregister.common.entity.TableNames;
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

    @Test
    public void whenGetJobStatusByName_thenDataAuditRowIsPersisted() throws Exception {
        val jobName = AdminJobType.APPLICATION_LISTS_DATABASE_JOB.name();

        // Remove earlier rows so the assertions below only inspect this GET request.
        dataAuditRepository.deleteAll();

        val responseSpec =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(WEB_CONTEXT + "/" + jobName),
                        createAdminToken().fetchTokenForRole());

        responseSpec.then().statusCode(200);

        // Verify the GET audit row persisted for the requested database job name.
        val persistedAuditRow =
                dataAuditRepository.findAll().stream()
                        .filter(row -> TableNames.DATABASE_JOBS.equals(row.getTableName()))
                        .filter(row -> "job_name".equals(row.getColumnName()))
                        .filter(
                                row ->
                                        AdminJobType.APPLICATION_LISTS_DATABASE_JOB
                                                .getValue()
                                                .equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a database_jobs.job_name audit row"
                                                        + " for GET /admin/jobs/{jobType}"));

        assertEquals("", persistedAuditRow.getOldValue());
        assertEquals(
                AdminAuditOperation.GET_DATABASE_JOB_STATUS_AUDIT_EVENT.getEventName(),
                persistedAuditRow.getEventName());
        assertEquals(
                AdminAuditOperation.GET_DATABASE_JOB_STATUS_AUDIT_EVENT.getType(),
                persistedAuditRow.getUpdateType());
    }
}
