package uk.gov.hmcts.appregister.controller.applicationentry;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.common.entity.repository.StandardApplicantRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ApplicationEntryControllerBulkUploadTest extends AbstractApplicationEntryCrudTest {

    private static final String BULK_UPLOAD_CSV = "/bulk-upload-application-list-entries-nle.csv";
    private static final int CSV_ROW_COUNT = 5;

    @Autowired private StandardApplicantRepository standardApplicantRepository;

    @Test
    void givenNleCsv_whenBulkUploadApplicationListEntries_thenCreatesEntries() throws Exception {
        UUID listId = getOpenApplicationListId();
        ensureCsvReferenceData(listId);
        int entryCountBefore = countEntriesForList(listId);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();

        Response response =
                given().header("Authorization", "Bearer " + token.getToken())
                        .accept("application/vnd.hmcts.appreg.v1+json")
                        .multiPart("file", csvFile(), "text/csv")
                        .post(
                                getLocalUrl(
                                        CREATE_ENTRY_CONTEXT
                                                + "/"
                                                + listId
                                                + "/entries/bulk-import"))
                        .andReturn();

        response.then().statusCode(202);
        JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);
        Assertions.assertEquals(JobType.BULK_UPLOAD_ENTRIES, acknowledgement.getType());

        waitForJobToComplete(tokenGenerator, acknowledgement.getId());

        Assertions.assertEquals(entryCountBefore + CSV_ROW_COUNT, countEntriesForList(listId));
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

    private void ensureCsvReferenceData(UUID listId) {
        unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findByUuidIncludingDelete(listId)
                                    .orElseThrow();
                    applicationList.setStatus(Status.OPEN);
                    applicationList.setDeleted(false);
                    applicationListRepository.save(applicationList);

                    StandardApplicant applicant = new StandardApplicant();
                    applicant.setApplicantCode("AW62958");
                    applicant.setApplicantStartDate(LocalDate.now().minusDays(1));
                    applicant.setApplicantEndDate(null);
                    applicant.setName("NLE test organisation");
                    applicant.setAddressLine1("Organisation Address Line 1");
                    applicant.setAddressLine2("Organisation Address Line 2");
                    applicant.setAddressLine3("Organisation Address Line 3");
                    applicant.setAddressLine4("Organisation Address Line 4");
                    applicant.setAddressLine5("Organisation Address Line 5");
                    applicant.setPostcode("WS1 1SY");
                    applicant.setEmailAddress("organisation-Test@test.cgi.com");
                    applicant.setTelephoneNumber("0207 6789012");
                    applicant.setMobileNumber("07776 567890");
                    applicant.setChangedBy(0L);
                    applicant.setChangedDate(OffsetDateTime.now());
                    standardApplicantRepository.save(applicant);
                    standardApplicantRepository.flush();
                });
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
