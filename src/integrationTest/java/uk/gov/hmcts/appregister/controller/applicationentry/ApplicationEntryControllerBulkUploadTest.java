package uk.gov.hmcts.appregister.controller.applicationentry;

import io.restassured.response.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.applicationentry.mapper.ApplicationListEntryEntityMapper;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryFeeStatusRepository;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
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

        waitForJobToComplete(tokenGenerator, acknowledgement.getId());

        Assertions.assertEquals(CSV_ROW_COUNT, countEntriesForList(listId));
        Assertions.assertEquals(expectedPersistedEntries(), persistedEntriesForList(listId));
        Assertions.assertEquals(expectedInitialFeeStatuses(), persistedFeeStatusesForList(listId));
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
                respondent.getForename1(),
                respondent.getForename2(),
                respondent.getForename3(),
                respondent.getSurname(),
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
                                "Anne",
                                "Louise",
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
                                "Morgan",
                                "Rae",
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
                                "Priya",
                                "Noor",
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

    private static List<PersistedFeeStatus> expectedInitialFeeStatuses() {
        return List.of(
                new PersistedFeeStatus((short) 1, "AD99001", FeeStatusType.DUE, null),
                new PersistedFeeStatus((short) 4, "MS99007", FeeStatusType.DUE, null));
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
                ApplicationListEntryEntityMapper.BULK_UPLOAD_YES,
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
