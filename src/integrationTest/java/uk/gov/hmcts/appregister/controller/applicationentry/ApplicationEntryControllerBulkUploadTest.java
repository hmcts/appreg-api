package uk.gov.hmcts.appregister.controller.applicationentry;

import io.restassured.response.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ApplicationEntryControllerBulkUploadTest extends AbstractApplicationEntryCrudTest {

    private static final String BULK_UPLOAD_CSV = "/bulk-upload-application-list-entries.csv";
    private static final int CSV_ROW_COUNT = 5;

    @Test
    void givenCsv_whenBulkUploadApplicationListEntries_thenCreatesEntries() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();

        UUID listId = createNewApplicationList(token);
        Assertions.assertEquals(0, countEntriesForList(listId));

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                        token,
                        "file",
                        csvFile(),
                        "text/csv");

        response.then().statusCode(202);
        JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);
        Assertions.assertEquals(JobType.BULK_UPLOAD_ENTRIES, acknowledgement.getType());

        waitForJobToComplete(tokenGenerator, acknowledgement.getId());

        Assertions.assertEquals(CSV_ROW_COUNT, countEntriesForList(listId));
    }

    private UUID createNewApplicationList(TokenAndJwksKey token) throws Exception {
        var createListRequest =
                new ApplicationListCreateDto()
                        .date(LocalDate.now().plusDays(1))
                        .time(LocalTime.of(10, 0))
                        .description("Bulk upload test list " + UUID.randomUUID())
                        .status(ApplicationListStatus.OPEN)
                        .courtLocationCode(VALID_COURT_CODE)
                        .durationHours(1)
                        .durationMinutes(0);

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT), token, createListRequest);

        response.then().statusCode(201);

        return response.as(ApplicationListGetDetailDto.class).getId();
    }

    private void waitForJobToComplete(TokenGenerator tokenGenerator, UUID jobId) {
        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl("jobs/" + jobId),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement jobStatus = jobResponse.as(JobAcknowledgement.class);

                    if (jobStatus.getStatus() == JobStatus1.FAILED) {
                        Assertions.fail(jobStatus.getErrorDescription());
                    }

                    return jobStatus.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));
    }

    private File csvFile() throws URISyntaxException {
        return new File(getClass().getResource(BULK_UPLOAD_CSV).toURI());
    }

    private int countEntriesForList(UUID listId) {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findByUuidIncludingDelete(listId)
                                    .orElseThrow();
                    return applicationListEntryRepository
                            .findByApplicationListId(applicationList.getId())
                            .size();
                });
    }
}
