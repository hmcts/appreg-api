package uk.gov.hmcts.appregister.controller.applicationentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.common.async.exception.JobError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.entity.repository.AsyncJobAppListEntryRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.ApplicationListCreateDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;
import uk.gov.hmcts.appregister.generated.model.ContactDetails;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.Organisation;
import uk.gov.hmcts.appregister.generated.model.Respondent;
import uk.gov.hmcts.appregister.generated.model.RespondentPerson;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

class ApplicationEntryControllerBulkUploadTest extends AbstractApplicationEntryCrudTest {

    private static final String BULK_UPLOAD_CSV = "/bulk-upload-application-list-entries.csv";
    private static final int CSV_ROW_COUNT = 5;

    @Autowired private AsyncJobAppListEntryRepository asyncJobAppListEntryRepository;
    @Autowired private AppListEntryFeeStatusRepository appListEntryFeeStatusRepository;

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

        JobAcknowledgement completedJob =
                AwaitilityUtil.waitForJobToReachTerminalStatus(
                        restAssuredClient,
                        getLocalUrl("jobs/" + acknowledgement.getId()),
                        tokenGenerator.fetchTokenForRole());
        Assertions.assertEquals(
                JobStatus1.COMPLETED, completedJob.getStatus(), completedJob.getErrorDescription());

        Assertions.assertEquals(CSV_ROW_COUNT, countEntriesForList(listId));
        Assertions.assertEquals(expectedApiEntries(), apiEntriesForList(listId, token));
        Assertions.assertEquals(expectedPersistedEntries(), persistedEntriesForList(listId));
        Assertions.assertEquals(expectedInitialFeeStatuses(), persistedFeeStatusesForList(listId));
    }

    @Test
    void givenInvalidBulkUploadHeader_whenBulkUploadApplicationListEntries_thenReturns400()
            throws Exception {
        TokenAndJwksKey token = createAdminToken().fetchTokenForRole();
        UUID listId = createNewApplicationList(token);

        try (var file = tempCsv("APPLICANT_CODE|APPLICATION_CODE\nAPP001|AP99001\n")) {
            Response response =
                    restAssuredClient.executePostRequest(
                            getLocalUrl(
                                    CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                            token,
                            "file",
                            file.file(),
                            "text/csv");

            response.then().statusCode(400);
            ProblemAssertUtil.assertEquals(
                    AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT.getCode(), response);
        }
    }

    @Test
    void givenBusinessInvalidBulkUploadRow_whenBulkUploadApplicationListEntries_thenJobFails()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();
        UUID listId = createNewApplicationList(token);

        try (var file =
                tempCsv(
                        "APPLICANT_CODE|RESP_TITLE|RESP_NAME_ORG|RESP_FORENAME1|RESP_FORENAME2"
                                + "|RESP_FORENAME3|RESP_SURNAME|RESP_ADDLINE1|RESP_ADDLINE2"
                                + "|RESP_ADDLINE3|RESP_ADDLINE4|RESP_ADDLINE5|RESP_POSTCODE"
                                + "|RESP_EMAIL|RESP_TEL|RESP_MOBILE|ACCOUNT_NUMBER"
                                + "|APPLICATION_CODE|APPLICATION_TEXT1|APPLICATION_TEXT2\n"
                                + "APP001||Alpha Holdings Ltd|||||1 Alpha Street|Suite 10"
                                + "|North Quarter|London|Greater London|AA1 1AA"
                                + "|alpha.holdings@example.com|0207 1111111|07771 111111"
                                + "|AC2023110001|ZZ99999||\n")) {
            Response response =
                    restAssuredClient.executePostRequest(
                            getLocalUrl(
                                    CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                            token,
                            "file",
                            file.file(),
                            "text/csv");

            response.then().statusCode(202);
            JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);

            JobAcknowledgement completedJob =
                    AwaitilityUtil.waitForJobToReachTerminalStatus(
                            restAssuredClient,
                            getLocalUrl("jobs/" + acknowledgement.getId()),
                            tokenGenerator.fetchTokenForRole());

            Assertions.assertEquals(JobStatus1.FAILED, completedJob.getStatus());
            assertThat(completedJob.getErrorDescription())
                    .isNotBlank()
                    .contains("\"rowNumber\":2")
                    .contains("\"location\":\"APPLICATION_CODE\"")
                    .contains("ZZ99999");
            Assertions.assertEquals(0, countEntriesForList(listId));
        }
    }

    @Test
    void givenFieldCountMismatchOnOnlyRow_whenJobPolled_thenReturnsStructuredHeaderError()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();
        UUID listId = createNewApplicationList(token);

        String header =
                "APPLICANT_CODE|RESP_TITLE|RESP_NAME_ORG|RESP_FORENAME1|RESP_FORENAME2"
                        + "|RESP_FORENAME3|RESP_SURNAME|RESP_ADDLINE1|RESP_ADDLINE2"
                        + "|RESP_ADDLINE3|RESP_ADDLINE4|RESP_ADDLINE5|RESP_POSTCODE"
                        + "|RESP_EMAIL|RESP_TEL|RESP_MOBILE|ACCOUNT_NUMBER"
                        + "|APPLICATION_CODE|APPLICATION_TEXT1|APPLICATION_TEXT2";
        String malformedRow = String.join("|", java.util.Collections.nCopies(21, ""));

        try (var file = tempCsv(header + "\n" + malformedRow + "\n")) {
            Response response =
                    restAssuredClient.executePostRequest(
                            getLocalUrl(
                                    CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                            token,
                            "file",
                            file.file(),
                            "text/csv");

            response.then().statusCode(202);
            JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);

            JobAcknowledgement completedJob =
                    AwaitilityUtil.waitForJobToReachTerminalStatus(
                            restAssuredClient,
                            getLocalUrl("jobs/" + acknowledgement.getId()),
                            tokenGenerator.fetchTokenForRole());

            Assertions.assertEquals(JobStatus1.FAILED, completedJob.getStatus());
            assertThat(completedJob.getErrorDescription())
                    .isEqualTo(
                            "[{\"rowNumber\":-1,\"location\":\"BULK_UPLOAD_ROW\","
                                    + "\"rejectedValue\":null,\"message\":\"Number of data fields "
                                    + "does not match number of headers.\",\"addressLine1\":null,"
                                    + "\"name\":null,\"errorType\":\"HEADER_ERROR\"}]")
                    .doesNotContain("Failed to process job");
            Assertions.assertEquals(0, countEntriesForList(listId));
        }
    }

    @Test
    void givenMissingApplicationList_whenBulkUploadApplicationListEntries_thenReturns404()
            throws Exception {
        TokenAndJwksKey token = createAdminToken().fetchTokenForRole();

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + UUID.fromString("00000000-0000-0000-0000-000000000001")
                                        + "/entries/bulk-import"),
                        token,
                        "file",
                        csvFile(),
                        "text/csv");

        response.then().statusCode(404);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    void givenClosedApplicationList_whenBulkUploadApplicationListEntries_thenReturns409()
            throws Exception {
        TokenAndJwksKey token = createAdminToken().fetchTokenForRole();

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT
                                        + "/"
                                        + getClosedApplicationListId()
                                        + "/entries/bulk-import"),
                        token,
                        "file",
                        csvFile(),
                        "text/csv");

        response.then().statusCode(409);
        ProblemDetail problemDetail = response.as(ProblemDetail.class);

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType().get(),
                problemDetail.getType());
    }

    @Test
    void givenLargeCsv6MB_whenBulkUploadApplicationListEntries_thenJobFails() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();
        UUID listId = createNewApplicationList(token);

        try (var file = tempCsv(6, "m")) {
            File csv = file.file();
            Response response =
                    restAssuredClient.executePostRequest(
                            getLocalUrl(
                                    CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                            token,
                            "file",
                            csv,
                            "text/csv");

            response.then().statusCode(413);
            ProblemDetail problemDetail = response.as(ProblemDetail.class);
            Assertions.assertEquals(
                    AppListEntryError.BULK_UPLOAD_FILE_TOO_LARGE.getCode().getType().get(),
                    problemDetail.getType());
        }
    }

    @Test
    void givenLargeCsv5MB_invalidCsv_whenBulkUploadApplicationListEntries_thenJobFails()
            throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();
        UUID listId = createNewApplicationList(token);

        try (var file = tempCsv(5, "m")) {
            File csv = file.file();
            Response response =
                    restAssuredClient.executePostRequest(
                            getLocalUrl(
                                    CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                            token,
                            "file",
                            csv,
                            "text/csv");

            response.then().statusCode(400);
            ProblemDetail detail = response.as(ProblemDetail.class);
            Assertions.assertEquals(
                    AppListEntryError.BULK_UPLOAD_INVALID_FILE_FORMAT.getCode().getType().get(),
                    detail.getType());
        }
    }

    @Test
    void givenSuccessfulBulkUpload_whenBulkUploadApplicationListEntries_thenSucceeds()
            throws Exception {
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

        assertThat(response.getStatusCode()).isEqualTo(202);

        JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);
        Assertions.assertEquals(JobType.BULK_UPLOAD_ENTRIES, acknowledgement.getType());

        JobStatus1 status = acknowledgement.getStatus();
        int retries = 0;
        int maxRetries = 5; // Set a maximum number of retries
        while (!status.equals(JobStatus1.COMPLETED) && retries < maxRetries) {
            Thread.sleep(1000);
            Response jobStatusResponse =
                    restAssuredClient.executeGetRequest(
                            getLocalUrl("jobs/" + acknowledgement.getId()), token);
            assertThat(jobStatusResponse.getStatusCode()).isEqualTo(200);
            JobAcknowledgement jobStatus = jobStatusResponse.as(JobAcknowledgement.class);
            status = jobStatus.getStatus();
            retries++;
        }

        String jobId = acknowledgement.getId().toString();
        assertThat(jobId).isNotEmpty();

        Response appListEntriesForJob =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/entries/bulk-import/" + jobId), token);

        assertThat(appListEntriesForJob.getStatusCode()).isEqualTo(200);
        UUID[] appListEntries = appListEntriesForJob.as(UUID[].class);
        assertThat(appListEntries).hasSizeGreaterThan(0);
    }

    @Test
    void givenNonExistentJobId_whenBulkUploadApplicationListEntries_thenFails() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();

        Response appListEntriesForJob =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT + "/entries/bulk-import/" + new UUID(0, 0)),
                        token);

        assertThat(appListEntriesForJob.getStatusCode()).isEqualTo(404);
        ProblemDetail detail = appListEntriesForJob.as(ProblemDetail.class);
        assertThat(detail.getType())
                .isEqualTo(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER.getCode().getType().get());
        assertThat(detail.getDetail())
                .isEqualTo("The requested job does not exist or it is not for the user");
    }

    @Test
    void givenWrongJobIdForUser_whenBulkUploadApplicationListEntries_thenFails() throws Exception {
        TokenGenerator tokenGenerator = createAdminToken();
        TokenAndJwksKey adminToken = tokenGenerator.fetchTokenForRole();

        UUID listId = createNewApplicationList(adminToken);
        Assertions.assertEquals(0, countEntriesForList(listId));

        Response response =
                restAssuredClient.executePostRequest(
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries/bulk-import"),
                        adminToken,
                        "file",
                        csvFile(),
                        "text/csv");

        assertThat(response.getStatusCode()).isEqualTo(202);

        JobAcknowledgement acknowledgement = response.as(JobAcknowledgement.class);
        Assertions.assertEquals(JobType.BULK_UPLOAD_ENTRIES, acknowledgement.getType());

        JobStatus1 status = acknowledgement.getStatus();
        while (!status.equals(JobStatus1.COMPLETED)) {
            Thread.sleep(1000);
            Response jobStatusResponse =
                    restAssuredClient.executeGetRequest(
                            getLocalUrl("jobs/" + acknowledgement.getId()), adminToken);
            assertThat(jobStatusResponse.getStatusCode()).isEqualTo(200);
            JobAcknowledgement jobStatus = jobStatusResponse.as(JobAcknowledgement.class);
            status = jobStatus.getStatus();
        }

        String jobId = acknowledgement.getId().toString();

        assertThat(jobId).isNotEmpty();

        when(provider.getUserId()).thenReturn("different-user-id");
        when(provider.getEmail()).thenReturn("different@user.com");
        when(provider.getRoles()).thenReturn(new String[] {"role"});

        Response appListEntriesForJob =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(
                                CREATE_ENTRY_CONTEXT + "/entries/bulk-import/" + new UUID(0, 0)),
                        getToken());

        assertThat(appListEntriesForJob.getStatusCode()).isEqualTo(404);
        ProblemDetail detail = appListEntriesForJob.as(ProblemDetail.class);
        assertThat(detail.getType())
                .isEqualTo(JobError.JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER.getCode().getType().get());
        assertThat(detail.getDetail())
                .isEqualTo("The requested job does not exist or it is not for the user");
    }

    private UUID createNewApplicationList(TokenAndJwksKey token) throws Exception {
        var createListRequest =
                new ApplicationListCreateDto()
                        .date(LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1))
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

    private File csvFile() throws URISyntaxException {
        return new File(getClass().getResource(BULK_UPLOAD_CSV).toURI());
    }

    private static AutoDeletingFile tempCsv(int size, String sizeSuffix) throws IOException {

        StringBuilder builder = new StringBuilder();
        long totalBytes = size;
        if (sizeSuffix.equalsIgnoreCase("k")) {
            totalBytes *= 1024;
        } else if (sizeSuffix.equalsIgnoreCase("m")) {
            totalBytes *= 1024 * 1024;
        }

        for (long i = 0; i < totalBytes; i++) {
            builder.append("\0");
        }

        return tempCsv(builder.toString());
    }

    private static AutoDeletingFile tempCsv(String content) throws IOException {
        File file = AppRegTempFileUtil.generateTempFile("bulk-upload-test");
        Files.writeString(file.toPath(), content);
        return new AutoDeletingFile(file);
    }

    private record AutoDeletingFile(File file) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            Files.deleteIfExists(file.toPath());
        }
    }

    private List<PersistedEntry> persistedEntriesForList(UUID listId) {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findByUuidIncludingDelete(listId)
                                    .orElseThrow();
                    return applicationListEntryRepository
                            .findByApplicationListId(applicationList.getId())
                            .stream()
                            .sorted(Comparator.comparing(ApplicationListEntry::getSequenceNumber))
                            .map(ApplicationEntryControllerBulkUploadTest::toPersistedEntry)
                            .toList();
                });
    }

    private List<ApiEntry> apiEntriesForList(UUID listId, TokenAndJwksKey token) throws Exception {
        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(CSV_ROW_COUNT),
                        Optional.of(0),
                        List.of(),
                        getLocalUrl(CREATE_ENTRY_CONTEXT + "/" + listId + "/entries"),
                        token);

        response.then().statusCode(200);

        EntryPage page = response.as(EntryPage.class);
        Assertions.assertEquals(CSV_ROW_COUNT, page.getContent().size());
        Assertions.assertEquals(CSV_ROW_COUNT, page.getTotalElements());

        return page.getContent().stream().map(entry -> toApiEntry(listId, entry)).toList();
    }

    private static ApiEntry toApiEntry(UUID listId, EntryGetSummaryDto entry) {
        Assertions.assertNotNull(entry.getId());
        Assertions.assertEquals(listId, entry.getListId());
        Assertions.assertEquals(ApplicationListStatus.OPEN, entry.getStatus());
        Assertions.assertFalse(entry.getIsResulted());
        assertThat(entry.getResulted()).isEmpty();
        Assertions.assertNotNull(entry.getDate());

        return new ApiEntry(
                entry.getSequenceNumber(),
                entry.getApplicationTitle(),
                entry.getIsFeeRequired(),
                valueOrNull(entry.getAccountNumber()),
                toApiRespondent(entry.getRespondent()),
                entry.getStatus());
    }

    private static PersistedRespondent toApiRespondent(Respondent respondent) {
        if (respondent.getOrganisation() != null) {
            Organisation organisation = respondent.getOrganisation();
            ContactDetails contactDetails = organisation.getContactDetails();
            return organisationRespondent(
                    organisation.getName(),
                    contactDetails.getAddressLine1(),
                    valueOrNull(contactDetails.getAddressLine2()),
                    valueOrNull(contactDetails.getAddressLine3()),
                    valueOrNull(contactDetails.getAddressLine4()),
                    valueOrNull(contactDetails.getAddressLine5()),
                    contactDetails.getPostcode(),
                    valueOrNull(contactDetails.getEmail()),
                    valueOrNull(contactDetails.getPhone()),
                    valueOrNull(contactDetails.getMobile()));
        }

        RespondentPerson person = respondent.getPerson();
        ContactDetails contactDetails = person.getContactDetails();
        return personRespondent(
                person.getName().getTitle(),
                person.getName().getFirstName(),
                valueOrNull(person.getName().getMiddleName()),
                null,
                person.getName().getLastName(),
                contactDetails.getAddressLine1(),
                valueOrNull(contactDetails.getAddressLine2()),
                valueOrNull(contactDetails.getAddressLine3()),
                valueOrNull(contactDetails.getAddressLine4()),
                valueOrNull(contactDetails.getAddressLine5()),
                contactDetails.getPostcode(),
                valueOrNull(contactDetails.getEmail()),
                valueOrNull(contactDetails.getPhone()),
                valueOrNull(contactDetails.getMobile()));
    }

    private static String valueOrNull(JsonNullable<String> value) {
        return value == null || !value.isPresent() ? null : value.get();
    }

    private static PersistedEntry toPersistedEntry(ApplicationListEntry entry) {
        return new PersistedEntry(
                entry.getSequenceNumber(),
                entry.getStandardApplicant().getApplicantCode(),
                entry.getApplicationCode().getCode(),
                entry.getAccountNumber(),
                entry.getBulkUpload(),
                entry.getApplicationListEntryWording(),
                toPersistedRespondent(entry.getRnameaddress()));
    }

    private static PersistedRespondent toPersistedRespondent(NameAddress respondent) {
        return new PersistedRespondent(
                respondent.getName(),
                respondent.getTitle(),
                respondent.getFirstName(),
                respondent.getMiddleName(),
                null,
                respondent.getLastName(),
                respondent.getAddress1(),
                respondent.getAddress2(),
                respondent.getAddress3(),
                respondent.getAddress4(),
                respondent.getAddress5(),
                respondent.getPostcode(),
                respondent.getEmailAddress(),
                respondent.getTelephoneNumber(),
                respondent.getMobileNumber());
    }

    private List<PersistedFeeStatus> persistedFeeStatusesForList(UUID listId) {
        return unitOfWork.inTransaction(
                () -> {
                    ApplicationList applicationList =
                            applicationListRepository
                                    .findByUuidIncludingDelete(listId)
                                    .orElseThrow();
                    return applicationListEntryRepository
                            .findByApplicationListId(applicationList.getId())
                            .stream()
                            .sorted(Comparator.comparing(ApplicationListEntry::getSequenceNumber))
                            .map(this::persistedFeeStatusesForEntry)
                            .flatMap(List::stream)
                            .toList();
                });
    }

    private List<PersistedFeeStatus> persistedFeeStatusesForEntry(ApplicationListEntry entry) {
        return appListEntryFeeStatusRepository.findByAppListEntryId(entry.getId()).stream()
                .map(feeStatus -> toPersistedFeeStatus(entry, feeStatus))
                .toList();
    }

    private static PersistedFeeStatus toPersistedFeeStatus(
            ApplicationListEntry entry, AppListEntryFeeStatus feeStatus) {
        Assertions.assertNotNull(feeStatus.getAlefsFeeStatusDate());
        Assertions.assertNotNull(feeStatus.getAlefsStatusCreationDate());
        return new PersistedFeeStatus(
                entry.getSequenceNumber(),
                entry.getApplicationCode().getCode(),
                feeStatus.getAlefsFeeStatus(),
                feeStatus.getAlefsPaymentReference());
    }

    private static List<PersistedEntry> expectedPersistedEntries() {
        return List.of(
                expectedEntry(
                        (short) 1,
                        "APP001",
                        "AD99001",
                        "AC2023110001",
                        "Request to copy documents",
                        organisationRespondent(
                                "Alpha Holdings Ltd",
                                "1 Alpha Street",
                                "Suite 10",
                                "North Quarter",
                                "London",
                                "Greater London",
                                "AA1 1AA",
                                "alpha.holdings@example.com",
                                "0207 1111111",
                                "07771 111111")),
                expectedEntry(
                        (short) 2,
                        "APP002",
                        "AP99001",
                        "AC2023110002",
                        "Notice of appeal in respect of a case heard on {2026-05-01}",
                        personRespondent(
                                "Ms",
                                "Beatrice",
                                "Anne Louise",
                                null,
                                "Baxter",
                                "2 Beta Road",
                                "Floor 2",
                                "West Arcade",
                                "Manchester",
                                "Lancashire",
                                "BB2 2BB",
                                "beatrice.baxter@example.com",
                                "0207 2222222",
                                "07772 222222")),
                expectedEntry(
                        (short) 3,
                        "APP003",
                        "CT99002",
                        "AC2023110003",
                        "Attends to swear a complaint for the issue of a summons for the debtor "
                                + "to answer an application for a liability order in relation to "
                                + "unpaid council tax (reference {COUNCIL-333})",
                        personRespondent(
                                "Dr",
                                "Caleb",
                                "Morgan Rae",
                                null,
                                "Carter",
                                "3 Gamma Avenue",
                                "Unit 3",
                                "East Park",
                                "Birmingham",
                                "West Midlands",
                                "CC3 3CC",
                                "caleb.carter@example.com",
                                "0207 3333333",
                                "07773 333333")),
                expectedEntry(
                        (short) 4,
                        "APP001",
                        "MS99007",
                        "AC2023110004",
                        "Application for a warrant to enter premises at {4 Delta Lane} "
                                + "for date {2026-04-27}",
                        organisationRespondent(
                                "Delta Advisory Group",
                                "4 Delta Lane",
                                "Block D",
                                "South Yard",
                                "Leeds",
                                "West Yorkshire",
                                "DD4 4DD",
                                "delta.advisory@example.com",
                                "0207 4444444",
                                "07774 444444")),
                expectedEntry(
                        (short) 5,
                        "APP002",
                        "SW99007",
                        "AC2023110005",
                        "Application for an order to allow the applicant to inspect or take "
                                + "copies of bankers books held by {Epsilon Bank} in respect of "
                                + "criminal proceedings at {Bristol Court}.",
                        personRespondent(
                                "Mrs",
                                "Evelyn",
                                "Priya Noor",
                                null,
                                "Edwards",
                                "5 Epsilon Close",
                                "Room 5",
                                "Central Court",
                                "Bristol",
                                "Somerset",
                                "EE5 5EE",
                                "evelyn.edwards@example.com",
                                "0207 5555555",
                                "07775 555555")));
    }

    private static List<ApiEntry> expectedApiEntries() {
        List<PersistedEntry> entries = expectedPersistedEntries();
        List<String> applicationTitles =
                List.of(
                        "Copy documents",
                        "Appeal to Crown Court",
                        "Issue of liability order summons - council tax",
                        "Copy documents",
                        "Inspection of Bankers' Books (criminal proceedings)");
        List<Boolean> feeRequired = List.of(true, false, false, true, false);

        return List.of(
                expectedApiEntry(entries.get(0), applicationTitles.get(0), feeRequired.get(0)),
                expectedApiEntry(entries.get(1), applicationTitles.get(1), feeRequired.get(1)),
                expectedApiEntry(entries.get(2), applicationTitles.get(2), feeRequired.get(2)),
                expectedApiEntry(entries.get(3), applicationTitles.get(3), feeRequired.get(3)),
                expectedApiEntry(entries.get(4), applicationTitles.get(4), feeRequired.get(4)));
    }

    private static List<PersistedFeeStatus> expectedInitialFeeStatuses() {
        return List.of(
                new PersistedFeeStatus((short) 1, "AD99001", FeeStatusType.DUE, null),
                new PersistedFeeStatus((short) 4, "MS99007", FeeStatusType.DUE, null));
    }

    private static ApiEntry expectedApiEntry(
            PersistedEntry entry, String applicationTitle, boolean feeRequired) {
        return new ApiEntry(
                entry.sequenceNumber().intValue(),
                applicationTitle,
                feeRequired,
                entry.accountNumber(),
                entry.respondent(),
                ApplicationListStatus.OPEN);
    }

    private static PersistedEntry expectedEntry(
            short sequenceNumber,
            String applicantCode,
            String applicationCode,
            String accountNumber,
            String wording,
            PersistedRespondent respondent) {
        return new PersistedEntry(
                sequenceNumber,
                applicantCode,
                applicationCode,
                accountNumber,
                YesOrNo.YES.getValue(),
                wording,
                respondent);
    }

    private static PersistedRespondent organisationRespondent(
            String name,
            String address1,
            String address2,
            String address3,
            String address4,
            String address5,
            String postcode,
            String email,
            String telephone,
            String mobile) {
        return new PersistedRespondent(
                name, null, null, null, null, null, address1, address2, address3, address4,
                address5, postcode, email, telephone, mobile);
    }

    private static PersistedRespondent personRespondent(
            String title,
            String forename1,
            String forename2,
            String forename3,
            String surname,
            String address1,
            String address2,
            String address3,
            String address4,
            String address5,
            String postcode,
            String email,
            String telephone,
            String mobile) {
        return new PersistedRespondent(
                null, title, forename1, forename2, forename3, surname, address1, address2, address3,
                address4, address5, postcode, email, telephone, mobile);
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

    private record ApiEntry(
            Integer sequenceNumber,
            String applicationTitle,
            Boolean feeRequired,
            String accountNumber,
            PersistedRespondent respondent,
            ApplicationListStatus status) {}

    private record PersistedEntry(
            Short sequenceNumber,
            String applicantCode,
            String applicationCode,
            String accountNumber,
            String bulkUpload,
            String wording,
            PersistedRespondent respondent) {}

    private record PersistedFeeStatus(
            Short sequenceNumber,
            String applicationCode,
            FeeStatusType feeStatus,
            String paymentReference) {}

    private record PersistedRespondent(
            String organisationName,
            String title,
            String forename1,
            String forename2,
            String forename3,
            String surname,
            String address1,
            String address2,
            String address3,
            String address4,
            String address5,
            String postcode,
            String email,
            String telephone,
            String mobile) {}
}
