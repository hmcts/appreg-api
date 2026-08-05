package uk.gov.hmcts.appregister.controller.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import io.restassured.response.Response;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegExceptionHandler;
import uk.gov.hmcts.appregister.common.log.AbstractOperationDurationAspect;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

class ReportingControllerPostTest extends BaseIntegration {
    private static final String FEES_REPORT_WEB_CONTEXT = "reports/fees/jobs";
    private static final String SEARCH_WARRANTS_REPORT_WEB_CONTEXT = "reports/search-warrants/jobs";
    private static final String ACTIVITY_AUDIT_REPORT_WEB_CONTEXT = "reports/activity-audit/jobs";
    private static final String DURATION_REPORT_WEB_CONTEXT = "reports/duration/jobs";
    private static final String WORKLOAD_REPORT_WEB_CONTEXT = "reports/workload/jobs";
    private static final String LIST_MAINTENANCE_REPORT_WEB_CONTEXT =
            "reports/list-maintenance/jobs";
    private static final String PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT =
            "reports/private-prosecutors-index/jobs";
    private static final String JOB_WEB_CONTEXT = "jobs/%s";
    private static final String DOWNLOAD_WEB_CONTEXT = "reports/jobs/%s/download";
    private static final String ALICE_DISPLAY_USERNAME = "alice@example.com";
    private static final String ALICE_USER_ID =
            "00000000-0000-0000-0000-000000000001:11111111-1111-1111-1111-111111111111";
    private static final String BOB_DISPLAY_USERNAME = "bob@example.com";
    private static final String BOB_USER_ID =
            "00000000-0000-0000-0000-000000000002:22222222-2222-2222-2222-222222222222";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    void givenValidActivityAuditReportRequest_whenCreatingReport_thenFilteredCsvCanBeDownloaded()
            throws Exception {
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "STD_APPLICANT_CODE",
                "",
                "{AW62958}",
                OffsetDateTime.now(java.time.ZoneOffset.UTC),
                ALICE_DISPLAY_USERNAME,
                ALICE_USER_ID);
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_NAME",
                "",
                "Second page value",
                OffsetDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(1),
                ALICE_DISPLAY_USERNAME,
                ALICE_USER_ID);
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_ADDRESSLINE1",
                "",
                "Third page value",
                OffsetDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(2),
                ALICE_DISPLAY_USERNAME,
                ALICE_USER_ID);
        insertDataAuditRow(
                "Update Application",
                "APPLICATION_LIST_ENTRIES",
                "STD_APPLICANT_CODE",
                "AW62958",
                "AW62959",
                OffsetDateTime.now(java.time.ZoneOffset.UTC),
                ALICE_DISPLAY_USERNAME,
                ALICE_USER_ID);
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_NAME",
                "",
                "Hidden",
                OffsetDateTime.now(java.time.ZoneOffset.UTC),
                BOB_DISPLAY_USERNAME,
                BOB_USER_ID);
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "AL_ID",
                "",
                "12345",
                OffsetDateTime.now(java.time.ZoneOffset.UTC),
                ALICE_DISPLAY_USERNAME,
                ALICE_USER_ID);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        ActivityAuditFilterDto request =
                new ActivityAuditFilterDto()
                        .dateFrom(today)
                        .dateTo(today)
                        .username(ALICE_DISPLAY_USERNAME)
                        .activityTypes(List.of(ActivityType.BULK_APPLICATION_UPLOAD));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(ACTIVITY_AUDIT_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                "dateFrom",
                today.toString());
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                "dateTo",
                today.toString());
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                "username",
                ALICE_DISPLAY_USERNAME);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT,
                "activityTypes",
                "BULK_APPLICATION_UPLOAD");
        assertOnlyReportParametersAuditedFor(
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.ACTIVITY_AUDIT_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    return jobResponse.as(JobAcknowledgement.class).getStatus()
                            == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Activity Audit Report");
            assertThat(report).contains("Bulk Application Upload");
            assertThat(report).contains("AW62958");
            assertThat(report).contains("Second page value");
            assertThat(report).contains("Third page value");
            assertThat(report).contains(ALICE_DISPLAY_USERNAME);
            assertThat(report).doesNotContain(ALICE_USER_ID);
            assertThat(report).doesNotContain("{AW62958}");
            assertThat(report).doesNotContain("Update Application");
            assertThat(report).doesNotContain("Hidden");
            assertThat(report).doesNotContain(BOB_DISPLAY_USERNAME);
            assertThat(report).doesNotContain("12345");
        }
    }

    @Test
    void givenReportCreatedFilter_whenCreatingActivityAuditReport_thenRelevantEventsAreReturned()
            throws Exception {
        createReportJobAndAwaitCompletion(
                WORKLOAD_REPORT_WEB_CONTEXT,
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2031, Month.JANUARY, 2))
                        .dateTo(LocalDate.of(2031, Month.JANUARY, 2)),
                JobType.WORKLOAD_REPORT);
        createReportJobAndAwaitCompletion(
                SEARCH_WARRANTS_REPORT_WEB_CONTEXT,
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2032, Month.FEBRUARY, 3))
                        .dateTo(LocalDate.of(2032, Month.FEBRUARY, 3))
                        .location(new LegacyReportLocation().courtLocationCode("CCC003")),
                JobType.SEARCH_WARRANTS_REPORT);

        String report =
                createAndDownloadActivityAuditReport(
                        new ActivityAuditFilterDto()
                                .dateFrom(LocalDate.now(java.time.ZoneOffset.UTC))
                                .dateTo(LocalDate.now(java.time.ZoneOffset.UTC))
                                .activityTypes(List.of(ActivityType.REPORT_CREATED)));

        assertThat(report).contains("Activity Audit Report");
        assertThat(report).contains("Create Workload Report");
        assertThat(report).contains("2031-01-02");
        assertThat(report).contains("Create Search Warrants Report");
        assertThat(report).contains("2032-02-03");
    }

    @Test
    void givenActivityAuditReportRequestContainsUnsupportedField_whenCreatingReport_thenBadRequest()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        ActivityAuditFilterDto request =
                new ActivityAuditFilterDto()
                        .dateFrom(today)
                        .dateTo(today)
                        .activityTypes(List.of(ActivityType.CREATE_APPLICATION_LIST));
        ObjectNode requestBody = mapper.valueToTree(request);
        requestBody.put("courtCode", "LOC123");

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(ACTIVITY_AUDIT_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        mapper.writeValueAsString(requestBody));

        createResponse.then().statusCode(400);
        ProblemDetail problemDetail = createResponse.as(ProblemDetail.class);
        Assertions.assertEquals("Unsupported request field: courtCode", problemDetail.getDetail());
    }

    @Test
    void givenFeesReportRequestContainsUnsupportedNestedField_whenCreatingReport_thenBadRequest()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1))
                        .location(new LegacyReportLocation().courtLocationCode("CCC003"));
        ObjectNode requestBody = mapper.valueToTree(request);
        ((ObjectNode) requestBody.path("location")).put("unexpected", "value");

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        mapper.writeValueAsString(requestBody));

        createResponse.then().statusCode(400);
        ProblemDetail problemDetail = createResponse.as(ProblemDetail.class);
        Assertions.assertEquals(
                "Unsupported request field: location.unexpected", problemDetail.getDetail());
    }

    @Test
    void givenValidFeesReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1))
                        .applicantName("Smith")
                        .location(new LegacyReportLocation().courtLocationCode("CCC003"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "dateFrom", "2018-05-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "dateTo", "2018-05-31");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "applicantName", "Smith");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "courtLocationCode", "CCC003");
        assertOnlyReportParametersAuditedFor(ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.FEES_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.FEES_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Fees Report");
        }
    }

    @Test
    void givenFeesReportApplicantNameMatchesPersonSurname_whenCreatingReport_thenCsvIncludesEntry()
            throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.MAY, 18);
        insertFeesReportApplication(
                listDate, null, "ArcPerson", "Singlefield", "ARC person fee wording");
        insertFeesReportApplication(
                listDate,
                "Arc Organisation Applicant Ltd",
                null,
                null,
                "ARC organisation fee wording");

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(listDate)
                        .dateTo(listDate)
                        .applicantName("Singlefield");

        String report = createFeesReportAndDownload(request);

        assertThat(report).contains("Fees Report");
        assertThat(report).contains("ArcPerson Singlefield");
        assertThat(report).doesNotContain("Arc Organisation Applicant Ltd");
    }

    @Test
    void givenFeesReportApplicantNameMatchesOrganisation_whenCreatingReport_thenCsvIncludesEntry()
            throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.MAY, 19);
        insertFeesReportApplication(
                listDate, null, "ArcPerson", "Unmatched", "ARC person fee wording");
        insertFeesReportApplication(
                listDate,
                "Arc Organisation Singlefield Ltd",
                null,
                null,
                "ARC organisation fee wording");

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(listDate)
                        .dateTo(listDate)
                        .applicantName("Organisation Singlefield");

        String report = createFeesReportAndDownload(request);

        assertThat(report).contains("Fees Report");
        assertThat(report).contains("Arc Organisation Singlefield Ltd");
        assertThat(report).doesNotContain("ArcPerson Unmatched");
    }

    @Test
    void givenFeesReportOtherLocationOnly_whenCreatingReport_thenCsvIsFilteredByOtherLocation()
            throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.MAY, 20);
        insertFeesReportApplication(
                listDate,
                null,
                "ArcPerson",
                "IncludedOtherLocation",
                "ARC included fee wording",
                "Temporary Fees Hall");
        insertFeesReportApplication(
                listDate,
                null,
                "ArcPerson",
                "ExcludedOtherLocation",
                "ARC excluded fee wording",
                "Permanent Fees Hall");

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(listDate)
                        .dateTo(listDate)
                        .location(
                                new LegacyReportLocation()
                                        .otherLocationDescription("temporary fees"));

        String report = createFeesReportAndDownload(request);

        assertThat(report).contains("Fees Report");
        assertThat(report).contains("ArcPerson IncludedOtherLocation");
        assertThat(report).contains("Temporary Fees Hall");
        assertThat(report).doesNotContain("ArcPerson ExcludedOtherLocation");
        assertThat(report).doesNotContain("Permanent Fees Hall");
    }

    @Test
    void givenUnknownCjaCode_whenCreatingFeesReport_thenBadRequestIsReturned() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("QX")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(
                createResponse.asString().contains("Criminal Justice Area not found"));
    }

    @Test
    void givenCourtAndCjaLocation_whenCreatingFeesReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .courtLocationCode("CCC003")
                                        .cjaCode("CD")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(
                createResponse
                        .asString()
                        .contains(
                                "Invalid location filter combination. Use 'courtLocationCode' "
                                        + "on its own, 'cjaCode' on its own, "
                                        + "'otherLocationDescription' on its own"));
    }

    @Test
    void givenUnknownCourtCode_whenCreatingFeesReport_thenBadRequestIsReturned() throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        assertThat(createResponse.asString()).contains("Court not found");
    }

    @Test
    void givenDuplicateCjaCode_whenCreatingFeesReport_thenConflictIsReturned() throws Exception {
        insertDuplicateCjaRows("Z1");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("Z1")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(409);
        Assertions.assertTrue(
                createResponse
                        .asString()
                        .contains(
                                "Multiple Criminal Justice Areas found when only one was expected"));
    }

    @Test
    void
            givenValidSearchWarrantsReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        SearchWarrantsReportFilterDto request =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1))
                        .dateFrom(LocalDate.of(2018, Month.MAY, 31))
                        .dateTo(LocalDate.of(2018, Month.MAY, 1))
                        .location(new LegacyReportLocation().courtLocationCode("CCC003"));
        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(SEARCH_WARRANTS_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT,
                "dateFrom",
                "2018-05-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT,
                "dateTo",
                "2018-05-31");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT,
                "courtLocationCode",
                "CCC003");
        assertOnlyReportParametersAuditedFor(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT);
        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.SEARCH_WARRANTS_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.SEARCH_WARRANTS_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));
        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());
        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Search Warrants Report");
        }
    }

    @Test
    void
            givenInvalidLocationCombination_whenCreatingSearchWarrantsReport_thenBadRequestIsLoggedWithProblemDetail()
                    throws Exception {
        LogCaptor exceptionHandlerLogs = LogCaptor.forClass(AppRegExceptionHandler.class);
        LogCaptor durationAspectLogs = LogCaptor.forClass(AbstractOperationDurationAspect.class);
        exceptionHandlerLogs.clearLogs();
        durationAspectLogs.clearLogs();

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        SearchWarrantsReportFilterDto request =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2025, Month.OCTOBER, 1))
                        .dateTo(LocalDate.of(2025, Month.OCTOBER, 31))
                        .location(
                                new LegacyReportLocation()
                                        .courtLocationCode("LOC123")
                                        .otherLocationDescription("Test Other Location"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(SEARCH_WARRANTS_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);

        ProblemDetail problemDetail = createResponse.as(ProblemDetail.class);
        Assertions.assertEquals(400, problemDetail.getStatus());
        Assertions.assertEquals(
                "Invalid location filter combination. Use 'courtLocationCode' on its own, "
                        + "'cjaCode' on its own, 'otherLocationDescription' on its own, "
                        + "both 'cjaCode' and 'otherLocationDescription', or no location fields.",
                problemDetail.getDetail());

        Assertions.assertTrue(
                exceptionHandlerLogs.getWarnLogs().stream()
                        .anyMatch(
                                log ->
                                        log.contains(
                                                        "[400]: Invalid location filter"
                                                                + " combination")
                                                && log.contains(
                                                        "Use 'courtLocationCode' on its own,"
                                                                + " 'cjaCode' on its own,"
                                                                + " 'otherLocationDescription' on"
                                                                + " its own")));
        Assertions.assertTrue(
                durationAspectLogs.getErrorLogs().stream()
                        .noneMatch(log -> log.contains("Exception occurred during execution")));
    }

    @Test
    void
            givenValidDurationReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.APRIL, 10);
        insertApplicationListRow(
                "CLOSED",
                listDate,
                "XCD123",
                "County Hall",
                "Duration report integration list",
                "Duration Court",
                2,
                45,
                3);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("CD")
                                        .otherLocationDescription("County Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(DURATION_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, "dateFrom", "2026-04-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, "dateTo", "2026-04-28");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT, "cjaCode", "CD");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "County Hall");
        assertOnlyReportParametersAuditedFor(
                ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.DURATION_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.DURATION_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Duration Report");
            assertThat(report).contains("10/04/2026");
            assertThat(report).contains("XCD123 - Duration Court");
            assertThat(report).contains("County Hall");
            assertThat(report).contains("Duration report integration list");
            assertThat(report).contains("2");
            assertThat(report).contains("45");
        }
    }

    @Test
    void givenUnknownCjaCode_whenCreatingDurationReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("QX")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(DURATION_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(
                createResponse.asString().contains("Criminal Justice Area not found"));
    }

    @Test
    void givenUnknownCourtCode_whenCreatingDurationReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(DURATION_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        assertThat(createResponse.asString()).contains("Court not found");
    }

    @Test
    void givenDuplicateCjaCode_whenCreatingDurationReport_thenConflictIsReturned()
            throws Exception {
        insertDuplicateCjaRows("Z2");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("Z2")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(DURATION_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(409);
        Assertions.assertTrue(
                createResponse
                        .asString()
                        .contains(
                                "Multiple Criminal Justice Areas found when only one was expected"));
    }

    @Test
    void
            givenValidWorkloadReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);

        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT, "dateFrom", "2026-04-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT, "dateTo", "2026-04-28");

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response response =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    JobAcknowledgement job = response.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.WORKLOAD_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Workload Report");
        }
    }

    @Test
    void
            givenValidWorkloadReportRequest_AppListAppListEntryIsDeletedNullCheck_ReportCanBeDownloaded()
                    throws Exception {

        // Setup
        long id = insertApplicationList(LocalDate.of(2026, Month.JANUARY, 1), "", "", "", null);
        insertEntry(LocalDate.of(2026, Month.JANUARY, 1), id, "Test applicant", 1, "N");
        insertEntry(LocalDate.of(2026, Month.JANUARY, 1), id, "Test applicant 2", 2, null);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.JANUARY, 1))
                        .dateTo(LocalDate.of(2026, Month.JANUARY, 31));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);

        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT, "dateFrom", "2026-01-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT, "dateTo", "2026-01-31");

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response response =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    JobAcknowledgement job = response.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.WORKLOAD_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Workload Report");
            assertThat(report).contains("Test applicant");
            assertThat(report).contains("Test applicant 2");
        }
    }

    @Test
    void
            givenValidWorkloadReportWithOtherLocationFilter_whenCreatingReport_thenJobIsMadeAndReportCanBeDownloaded()
                    throws Exception {

        val listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        LocalDate.of(2026, Month.APRIL, 15),
                        "TH",
                        "Town Hall",
                        "Workload Report - Other Location",
                        "Workload Court",
                        0,
                        0,
                        3);

        val entryId =
                insertEntry(
                        LocalDate.of(2026, Month.APRIL, 15),
                        listId,
                        "Workload Report Applicant",
                        1,
                        "N");
        insertOfficial(entryId, "M", "Mr", "Workload", "Magistrate");
        insertOfficial(entryId, "M", "Mrs", "Magistrate", "Workload");
        insertOfficial(entryId, "M", "Mr", "Test", "Workload");
        insertOfficial(entryId, "C", "Mr", "T", "Jones");

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription("Town Hall");
        location.setCjaCode("CD");
        location.setCourtLocationCode(null);

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 15))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 15))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response response =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    JobAcknowledgement job = response.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.WORKLOAD_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Workload Report");
            assertThat(report).contains("Town Hall");
            assertThat(report).contains("CD");
        }
    }

    @Test
    void
            givenWorkloadReportCjaAndOtherLocationFilter_whenCreatingReport_thenCsvIsFilteredByBothValues()
                    throws Exception {
        val listDate = LocalDate.of(2026, Month.JUNE, 19);
        val includedApplicant = "ARCPOC 1463 Workload CJA Other Included";
        val excludedApplicant = "ARCPOC 1463 Workload CJA Other Excluded";
        val matchingLocation = "ARCPOC 1463 Shared Hall";
        val excludedCjaId = insertCjaRow("W9", "ARCPOC 1463 Excluded CJA");

        val includedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD146",
                        matchingLocation,
                        "Workload Report - CJA And Other Included",
                        "Workload Included Court",
                        0,
                        0,
                        3);
        insertEntry(listDate, includedListId, includedApplicant, 1, "N");

        val excludedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XW9146",
                        matchingLocation,
                        "Workload Report - CJA And Other Excluded",
                        "Workload Excluded Court",
                        0,
                        0,
                        excludedCjaId.intValue());
        insertEntry(listDate, excludedListId, excludedApplicant, 1, "N");

        val report =
                createAndDownloadWorkloadReport(
                        new WorkloadFilterDto()
                                .dateFrom(listDate)
                                .dateTo(listDate)
                                .location(
                                        new LegacyReportLocation()
                                                .cjaCode("CD")
                                                .otherLocationDescription("arcpoc 1463 shared")));

        assertThat(report).contains("Workload Report");
        assertThat(report).contains(includedApplicant);
        assertThat(report).contains(matchingLocation);
        assertThat(report).doesNotContain(excludedApplicant);
        assertThat(report).doesNotContain("Workload Excluded Court");
    }

    @Test
    void
            givenValidWorkloadReportWithJustCJACode_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {

        val listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        LocalDate.of(2026, Month.APRIL, 16),
                        "TH",
                        "Town Hall",
                        "Workload Report - CJA Code Only",
                        "Workload Court",
                        0,
                        0,
                        3);

        val entryId =
                insertEntry(
                        LocalDate.of(2026, Month.APRIL, 16),
                        listId,
                        "Workload Report Applicant",
                        1,
                        "N");
        insertOfficial(entryId, "M", "Mr", "Workload", "Magistrate");
        insertOfficial(entryId, "M", "Mrs", "Magistrate", "Workload");
        insertOfficial(entryId, "M", "Mr", "Test", "Workload");
        insertOfficial(entryId, "C", "Mr", "T", "Jones");

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription(null);
        location.setCjaCode("CD");
        location.setCourtLocationCode(null);

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 16))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 16))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response response =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    JobAcknowledgement job = response.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.WORKLOAD_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Workload Report");
            assertThat(report).contains("Town Hall");
            assertThat(report).contains("CD");
        }
    }

    @Test
    void givenWorkloadReportOtherLocationOnly_whenCreatingReport_thenCsvIsFilteredByOtherLocation()
            throws Exception {
        val listDate = LocalDate.of(2026, Month.JUNE, 18);
        val includedApplicant = "ARCPOC 1403 Workload Included";
        val excludedApplicant = "ARCPOC 1403 Workload Excluded";
        val includedLocation = "ARCPOC 1403 Workload Hall";
        val excludedLocation = "ARCPOC 1403 Different Hall";

        val includedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "WLD140",
                        includedLocation,
                        "Workload Report - Other Location Only Included",
                        "Workload Court",
                        0,
                        0,
                        3);
        insertEntry(listDate, includedListId, includedApplicant, 1, "N");

        val excludedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "WLD141",
                        excludedLocation,
                        "Workload Report - Other Location Only Excluded",
                        "Workload Court",
                        0,
                        0,
                        3);
        insertEntry(listDate, excludedListId, excludedApplicant, 1, "N");

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(listDate)
                        .dateTo(listDate)
                        .location(
                                new LegacyReportLocation()
                                        .otherLocationDescription("arcpoc 1403 workload"));

        val report = createAndDownloadWorkloadReport(request);

        assertThat(report).contains("Workload Report");
        assertThat(report).contains(includedApplicant);
        assertThat(report).contains(includedLocation);
        assertThat(report).doesNotContain(excludedApplicant);
        assertThat(report).doesNotContain(excludedLocation);
    }

    @Test
    // Shows that magistrate columns should be based on official order, not list entry sequence.
    public void
            givenSingleMagistrateOnSecondListEntry_whenCreatingWorkloadReport_thenMagistrateAppearsInFirstColumn()
                    throws Exception {
        val listDate = LocalDate.of(2026, Month.SEPTEMBER, 1); // fixes report date
        val applicantName = "Workload Second Entry Applicant"; // identifies target row
        val listId =
                insertApplicationList(
                        listDate,
                        "WLD001",
                        "Workload multi resolution list",
                        "Workload Court",
                        "N");
        val entryId =
                insertEntry(listDate, listId, applicantName, 2, "N"); // creates second list entry
        insertOfficial(entryId, "M", "Mr", "Solo", "Magistrate"); // adds one magistrate

        val row =
                workloadRow(
                        createAndDownloadWorkloadReport(
                                new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate)),
                        applicantName); // reads CSV row

        Assertions.assertEquals("Mr Solo Magistrate", row.get("JP1")); // expects first magistrate
        Assertions.assertEquals("", row.get("JP2")); // expects no second magistrate
        Assertions.assertEquals("", row.get("JP3")); // expects no third magistrate
    }

    @Test
    // Shows that multiple magistrates on the same entry should use separate columns.
    public void
            givenMultipleMagistratesOnSameEntry_whenCreatingWorkloadReport_thenEachMagistrateAppearsInSeparateColumn()
                    throws Exception {
        val listDate = LocalDate.of(2026, Month.SEPTEMBER, 2); // fixes report date
        val applicantName = "Workload Multiple Magistrates Applicant";
        val appListId =
                insertApplicationList(
                        listDate,
                        "WLD002",
                        "Workload Court",
                        applicantName,
                        "N"); // creates list// identifies target row
        val entryId =
                insertApplicationListEntry(
                        appListId,
                        applicationCodeId("MX99010"),
                        insertNameAddress(
                                "Workload Multiple Magistrates Applicant",
                                null,
                                null,
                                null,
                                "Test Road"),
                        insertNameAddress("", "Test", null, "Respondent", "Test Road"),
                        listDate); // creates first list entry
        insertOfficial(entryId, "M", "Mr", "First", "Magistrate"); // adds first magistrate
        insertOfficial(entryId, "M", "Mr", "Second", "Magistrate"); // adds second magistrate

        val row =
                workloadRow(
                        createAndDownloadWorkloadReport(
                                new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate)),
                        applicantName); // reads CSV row

        Assertions.assertEquals("Mr First Magistrate", row.get("JP1")); // expects first magistrate
        Assertions.assertEquals(
                "Mr Second Magistrate", row.get("JP2")); // expects second magistrate
        Assertions.assertEquals("", row.get("JP3")); // expects no third magistrate
    }

    @Test
    void givenCourtProvidedWithCJAFilter_whenCreatingWorkloadReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription(null);
        location.setCjaCode("01");
        location.setCourtLocationCode("TEST123");

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
    }

    @Test
    void givenWorkloadReportCourtOnly_whenCreatingReport_thenCsvIsFilteredByCourt()
            throws Exception {
        val listDate = LocalDate.of(2026, Month.JUNE, 20);
        val includedApplicant = "ARCPOC 1463 Workload Court Included";
        val excludedApplicant = "ARCPOC 1463 Workload Court Excluded";

        val includedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "CCC003",
                        null,
                        "Workload Report - Court Only Included",
                        "Cardiff Crown Court",
                        0,
                        0,
                        3);
        insertEntry(listDate, includedListId, includedApplicant, 1, "N");

        val excludedListId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "BCC006",
                        null,
                        "Workload Report - Court Only Excluded",
                        "Bristol Crown Court",
                        0,
                        0,
                        3);
        insertEntry(listDate, excludedListId, excludedApplicant, 1, "N");

        val report =
                createAndDownloadWorkloadReport(
                        new WorkloadFilterDto()
                                .dateFrom(listDate)
                                .dateTo(listDate)
                                .location(new LegacyReportLocation().courtLocationCode("CCC003")));

        assertThat(report).contains("Workload Report");
        assertThat(report).contains(includedApplicant);
        assertThat(report).contains("CCC003 - Cardiff Crown Court");
        assertThat(report).doesNotContain(excludedApplicant);
        assertThat(report).doesNotContain("BCC006 - Bristol Crown Court");
    }

    @Test
    void
            givenCourtProvidedWithOtherLocationFilter_whenCreatingWorkloadReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription("test");
        location.setCjaCode(null);
        location.setCourtLocationCode("TEST123");

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
    }

    @Test
    void
            givenWorkloadEntryHasMultipleResolutionsAndOfficials_whenCreatingReport_thenSingleCsvRowContainsAllData()
                    throws Exception {
        val listDate = LocalDate.of(2026, Month.AUGUST, 17);
        val applicantName = "Workload Multi Resolution Applicant";
        val applicantId = insertNameAddress(applicantName, null, null, null, "Workload Street");
        val respondentId =
                insertNameAddress("Workload Respondent", null, null, null, "Respondent Street");
        val listId =
                insertApplicationList(
                        listDate,
                        "WLD001",
                        "Workload multi resolution list",
                        "Workload Court",
                        "N");
        val entryId =
                insertApplicationListEntry(
                        listId, applicationCodeId("MX99010"), applicantId, respondentId, listDate);

        insertResolutionCodesAndResult(entryId, "CASE");
        insertResolutionCodesAndResult(entryId, "AUTH");
        // Resolutions and officials are both one-to-many joins; the report should still emit
        // one workload row per application list entry.
        insertOfficial(entryId, "M", "Mr", "Jill", "Magistrate");
        insertOfficial(entryId, "C", "Mr", "Casey", "Clerk");

        val report =
                createAndDownloadWorkloadReport(
                        new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate));
        val applicantRows = report.lines().filter(line -> line.contains(applicantName)).toList();

        Assertions.assertEquals(1, applicantRows.size()); // asserts one row per entry
        assertThat(applicantRows.getFirst()).contains("CASE"); // includes first result
        assertThat(applicantRows.getFirst()).contains("AUTH"); // includes second result
        assertThat(applicantRows.getFirst()).contains("Jill Magistrate"); // includes JP
        Assertions.assertTrue(
                applicantRows.getFirst().contains("Casey Clerk")); // includes official
    }

    @Test
    void givenValidPrivateProsecutorsIndexRequest_whenCreatingReport_thenCsvCanBeDownloaded()
            throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.APRIL, 11);
        insertPrivateProsecutorsIndexApplication(listDate);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28))
                        .applicantSurname("Legacy")
                        .respondentOrganisationName("Respondent Org")
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("CD")
                                        .otherLocationDescription("Private Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "dateFrom",
                "2026-04-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "dateTo",
                "2026-04-28");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "applicantSurname",
                "Legacy");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "respondentOrganisationName",
                "Respondent Org");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "cjaCode",
                "CD");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "Private Hall");
        assertOnlyReportParametersAuditedFor(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(
                            JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Private Prosecution Index Report");
            assertThat(report).contains("11/04/2026");
            assertThat(report).contains("XCD999 - Private Court");
            assertThat(report).contains("Private Hall");
            assertThat(report).contains("CD");
            assertThat(report).contains("Legacy");
            assertThat(report).contains("Private");
            assertThat(report).contains("Respondent Org Ltd");
            assertThat(report).contains("Private wording");
            assertThat(report).contains("CASE");
            assertThat(report).contains("AUTH");
            assertThat(report).contains("Private notes");
            assertThat(report).doesNotContain("{wording}");
        }
    }

    @Test
    void
            givenValidPrivateProsecutorsIndexRequestForStandardApplicant_whenCreatingReport_thenCsvCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.APRIL, 12);
        insertPrivateProsecutorsIndexStandardApplicantApplication(listDate);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 28))
                        .standardApplicantName("John")
                        .respondentOrganisationName("Standard Respondent")
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("CD")
                                        .otherLocationDescription("Standard Private Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "standardApplicantName",
                "John");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "respondentOrganisationName",
                "Standard Respondent");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "cjaCode",
                "CD");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "Standard Private Hall");

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(
                            JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Private Prosecution Index Report");
            assertThat(report).contains("12/04/2026");
            assertThat(report).contains("XCD998 - Standard Private Court");
            assertThat(report).contains("Standard Respondent Ltd");
            assertThat(report).contains("Standard private wording");
            assertThat(report).contains("REF");
            assertThat(report).contains("Standard private notes");
            assertThat(report).contains("John Smith");
            assertThat(report).contains("CD,,,John Smith,");
        }
    }

    @Test
    void
            givenDateOnlyPrivateProsecutorsIndexRequest_whenCreatingReport_thenStandardApplicantNameIsPopulated()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.APRIL, 13);
        insertPrivateProsecutorsIndexIndividualStandardApplicantApplication(listDate);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto().dateFrom(listDate).dateTo(listDate);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(
                            JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Private Prosecution Index Report");
            assertThat(report).contains("13/04/2026");
            assertThat(report).contains("XCD997 - Individual Standard Private Court");
            assertThat(report).contains("Jane Doe");
            assertThat(report).contains("CD,,,Jane Doe,");
        }
    }

    @Test
    void
            givenWhitespaceOnlyPrivateProsecutorsIndexFilters_whenCreatingReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .applicantSurname(" ")
                        .respondentFirstName(" ")
                        .location(new LegacyReportLocation().cjaCode(" "));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        assertThat(createResponse.asString()).contains("Validation failed for fields:");
        assertThat(createResponse.asString()).contains("applicantSurname");
        assertThat(createResponse.asString()).contains("respondentFirstName");
        assertThat(createResponse.asString()).contains("location.cjaCode");
    }

    @Test
    void
            givenPrivateProsecutorsIndexFilterContainsInternalWhitespace_whenCreatingReport_thenCsvCanBeDownloaded()
                    throws Exception {
        TokenAndJwksKey token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .applicantSurname("x y");

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT), token, request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT,
                "applicantSurname",
                "x y");
        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.PRIVATE_PROSECUTORS_INDEX_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    return jobResponse.as(JobAcknowledgement.class).getStatus()
                            == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("Private Prosecution Index Report");
            Assertions.assertEquals(2, report.lines().count());
        }
    }

    @Test
    void
            givenValidListMaintenanceReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, Month.APRIL, 11);
        Long matchingListId =
                insertApplicationListRowReturningId(
                        "OPEN",
                        listDate,
                        "XCD123",
                        "County Hall",
                        "List maintenance integration",
                        "Maintenance Court",
                        0,
                        0,
                        3);
        insertApplicationListEntryRows(matchingListId, 2, "N");
        insertApplicationListEntryRows(matchingListId, 1, "Y");
        insertApplicationListRow(
                "CLOSED",
                listDate,
                "XCD123",
                "County Hall",
                "Closed maintenance list",
                "Maintenance Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                listDate,
                "XCD123",
                "County Hall",
                "Unmatched report list",
                "Maintenance Court",
                0,
                0,
                3);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2026, Month.APRIL, 30))
                        .dateTo(LocalDate.of(2026, Month.APRIL, 1))
                        .listDescription("MAINTENANCE")
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("CD")
                                        .otherLocationDescription("County Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(202);
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                "dateFrom",
                "2026-04-01");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                "dateTo",
                "2026-04-30");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                "listDescription",
                "MAINTENANCE");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT, "cjaCode", "CD");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "County Hall");
        assertOnlyReportParametersAuditedFor(
                ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT);

        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.LIST_MAINTENANCE_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    tokenGenerator.fetchTokenForRole());

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.LIST_MAINTENANCE_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())),
                        tokenGenerator.fetchTokenForRole());

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            String report = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(report).contains("List Maintenance Report");
            Assertions.assertTrue(
                    report.contains(
                            "List Date,List Court House Name,List Other Location,CJA Code,"
                                    + "List Description,List Status,No Of Application Entries"));
            assertThat(report).contains("11/04/2026");
            assertThat(report).contains("XCD123 - Maintenance Court");
            assertThat(report).contains("County Hall");
            assertThat(report).contains("CD");
            assertThat(report).contains("List maintenance integration,OPEN,2");
            assertThat(report).doesNotContain("Closed maintenance list");
            assertThat(report).doesNotContain("Unmatched report list");
        }
    }

    @Test
    void givenListMaintenanceRowsOnDateBounds_whenNoLocationFilter_thenBothBoundsAreIncluded()
            throws Exception {
        String description = "LM boundary report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 1),
                "AAA001",
                null,
                description,
                "Boundary Start Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 30),
                "AAA002",
                null,
                description,
                "Boundary End Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JULY, 1),
                "AAA003",
                null,
                description,
                "Outside Boundary Court",
                0,
                0,
                3);

        String report =
                createAndDownloadListMaintenanceReport(
                        new ListMaintenanceFilterDto()
                                .dateFrom(LocalDate.of(2026, Month.JUNE, 1))
                                .dateTo(LocalDate.of(2026, Month.JUNE, 30))
                                .listDescription(description));

        assertThat(report).contains("01/06/2026,AAA001 - Boundary Start Court");
        assertThat(report).contains("30/06/2026,AAA002 - Boundary End Court");
        assertThat(report).doesNotContain("Outside Boundary Court");
    }

    @Test
    void givenCourtFilter_whenCreatingListMaintenanceReport_thenOnlyMatchingCourtIsReturned()
            throws Exception {
        String description = "LM court filter report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 15),
                "CCC003",
                null,
                description,
                "Cardiff Crown Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 15),
                "BCC006",
                null,
                description,
                "Bristol Crown Court",
                0,
                0,
                3);

        String report =
                createAndDownloadListMaintenanceReport(
                        new ListMaintenanceFilterDto()
                                .dateFrom(LocalDate.of(2026, Month.JUNE, 1))
                                .dateTo(LocalDate.of(2026, Month.JUNE, 30))
                                .listDescription(description)
                                .location(new LegacyReportLocation().courtLocationCode("CCC003")));

        assertThat(report).contains("CCC003 - Cardiff Crown Court");
        assertThat(report).doesNotContain("BCC006 - Bristol Crown Court");
    }

    @Test
    void
            givenOtherLocationAndCjaFilter_whenCreatingListMaintenanceReport_thenOnlyMatchingRowIsReturned()
                    throws Exception {
        String description = "LM other location report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 15),
                null,
                "Village Hall",
                description,
                null,
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, Month.JUNE, 15),
                null,
                "Village Hall",
                description,
                null,
                0,
                0,
                4);

        String report =
                createAndDownloadListMaintenanceReport(
                        new ListMaintenanceFilterDto()
                                .dateFrom(LocalDate.of(2026, Month.JUNE, 1))
                                .dateTo(LocalDate.of(2026, Month.JUNE, 30))
                                .listDescription(description)
                                .location(
                                        new LegacyReportLocation()
                                                .cjaCode("CD")
                                                .otherLocationDescription("village")));

        assertThat(report).contains("Village Hall,CD," + description);
        assertThat(report).doesNotContain("Village Hall,CE," + description);
    }

    @Test
    void givenNoMatchingRows_whenCreatingListMaintenanceReport_thenEmptyCsvIsReturned()
            throws Exception {
        String report =
                createAndDownloadListMaintenanceReport(
                        new ListMaintenanceFilterDto()
                                .dateFrom(LocalDate.of(2026, Month.JUNE, 1))
                                .dateTo(LocalDate.of(2026, Month.JUNE, 30))
                                .listDescription("LM no matching report rows"));

        assertThat(report).contains("List Maintenance Report");
        Assertions.assertTrue(
                report.contains(
                        "List Date,List Court House Name,List Other Location,CJA Code,"
                                + "List Description,List Status,No Of Application Entries"));
        Assertions.assertEquals(2, report.lines().count());
    }

    @Test
    void givenUnknownCjaCode_whenCreatingListMaintenanceReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("QX")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(
                createResponse.asString().contains("Criminal Justice Area not found"));
    }

    @Test
    void givenUnknownCourtCode_whenCreatingListMaintenanceReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        assertThat(createResponse.asString()).contains("Court not found");
    }

    @Test
    void givenDuplicateCjaCode_whenCreatingListMaintenanceReport_thenConflictIsReturned()
            throws Exception {
        insertDuplicateCjaRows("Z3");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .location(
                                new LegacyReportLocation()
                                        .cjaCode("Z3")
                                        .otherLocationDescription("Town Hall"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(409);
        Assertions.assertTrue(
                createResponse
                        .asString()
                        .contains(
                                "Multiple Criminal Justice Areas found when only one was expected"));
    }

    @Test
    void givenWhitespaceOnlyListMaintenanceFilters_whenCreatingReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, Month.MAY, 1))
                        .dateTo(LocalDate.of(2018, Month.MAY, 31))
                        .listDescription(" ")
                        .location(new LegacyReportLocation().cjaCode(" "));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        assertThat(createResponse.asString()).contains("Validation failed for fields:");
        assertThat(createResponse.asString()).contains("listDescription");
        assertThat(createResponse.asString()).contains("location.cjaCode");
    }

    private void insertDataAuditRow(
            String eventName,
            String tableName,
            String columnName,
            String oldValue,
            String newValue,
            OffsetDateTime createdDate,
            String displayUserName,
            String userId) {
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.data_audit (
                        data_id,
                        schema_name,
                        table_name,
                        column_name,
                        old_value,
                        new_value,
                        user_id,
                        link,
                        created_date,
                        old_clob_value,
                        new_clob_value,
                        related_key,
                        update_type,
                        data_type,
                        case_id,
                        related_items_identifier,
                        related_items_identifier_index,
                        event_name,
                        user_name
                    )
                    VALUES (
                        nextval('%s.add_dataaudit_event'),
                        'appreg',
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        NULL,
                        ?,
                        NULL,
                        NULL,
                        NULL,
                        'S',
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        ?,
                        ?
                    )
                    """,
                        schema, schema),
                tableName,
                columnName,
                oldValue,
                newValue,
                displayUserName,
                createdDate,
                eventName,
                userId);
    }

    private void insertDuplicateCjaRows(String code) {
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.criminal_justice_area (cja_id, cja_code, cja_description)
                    VALUES
                        (nextval('%s.cja_seq'), ?, ?),
                        (nextval('%s.cja_seq'), ?, ?)
                    """,
                        schema, schema, schema),
                code,
                "Duplicate CJA 1",
                code,
                "Duplicate CJA 2");
    }

    private Long insertCjaRow(String code, String description) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.criminal_justice_area (cja_id, cja_code, cja_description)
                    VALUES (nextval('%s.cja_seq'), ?, ?)
                    RETURNING cja_id
                    """,
                        schema, schema),
                Long.class,
                code,
                description);
    }

    private void insertApplicationListRow(
            String status,
            LocalDate listDate,
            String courthouseCode,
            String otherCourthouse,
            String listDescription,
            String courthouseName,
            int durationHours,
            int durationMinutes,
            int cjaId) {
        Long listId =
                jdbcTemplate.queryForObject(
                        String.format("SELECT nextval('%s.al_seq')", schema), Long.class);
        insertApplicationListRow(
                listId,
                status,
                listDate,
                courthouseCode,
                otherCourthouse,
                listDescription,
                courthouseName,
                durationHours,
                durationMinutes,
                cjaId);
    }

    private void insertApplicationListRow(
            Long listId,
            String status,
            LocalDate listDate,
            String courthouseCode,
            String otherCourthouse,
            String listDescription,
            String courthouseName,
            int durationHours,
            int durationMinutes,
            int cjaId) {
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.application_lists (
                        al_id,
                        application_list_status,
                        application_list_date,
                        application_list_time,
                        courthouse_code,
                        other_courthouse,
                        list_description,
                        version,
                        changed_by,
                        changed_date,
                        user_name,
                        courthouse_name,
                        duration_hour,
                        duration_minute,
                        cja_cja_id
                    )
                    VALUES (
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test',
                        ?,
                        ?,
                        ?,
                        ?
                    )
                    """,
                        schema),
                listId,
                status,
                listDate,
                listDate.atTime(10, 0),
                courthouseCode,
                otherCourthouse,
                listDescription,
                courthouseName,
                durationHours,
                durationMinutes,
                cjaId);
    }

    private Long insertApplicationListRowReturningId(
            String status,
            LocalDate listDate,
            String courthouseCode,
            String otherCourthouse,
            String listDescription,
            String courthouseName,
            int durationHours,
            int durationMinutes,
            int cjaId) {
        Long listId =
                jdbcTemplate.queryForObject(
                        String.format("SELECT nextval('%s.al_seq')", schema), Long.class);
        insertApplicationListRow(
                listId,
                status,
                listDate,
                courthouseCode,
                otherCourthouse,
                listDescription,
                courthouseName,
                durationHours,
                durationMinutes,
                cjaId);
        return listId;
    }

    private void insertApplicationListEntryRows(Long applicationListId, int count, String deleted) {
        Long applicationCodeId = applicationCodeId("MX99010");
        for (int index = 0; index < count; index++) {
            jdbcTemplate.update(
                    String.format(
                            """
                        INSERT INTO %s.application_list_entries (
                            ale_id,
                            al_al_id,
                            ac_ac_id,
                            application_list_entry_wording,
                            entry_rescheduled,
                            version,
                            changed_by,
                            changed_date,
                            sequence_number,
                            lodgement_date,
                            is_deleted
                        )
                        VALUES (
                            nextval('%s.ale_seq'),
                            ?,
                            ?,
                            ?,
                            'N',
                            1,
                            0,
                            CURRENT_TIMESTAMP,
                            ?,
                            CURRENT_TIMESTAMP,
                            ?
                        )
                        """,
                            schema, schema),
                    applicationListId,
                    applicationCodeId,
                    "List maintenance entry " + index,
                    index + 1,
                    deleted);
        }
    }

    private void insertPrivateProsecutorsIndexApplication(LocalDate listDate) {
        long applicantId =
                insertNameAddressRow(null, "Private", "Middle", "Legacy", "Applicant Street");
        long respondentId =
                insertNameAddressRow("Respondent Org Ltd", null, null, null, "Respondent Street");
        long listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD999",
                        "Private Hall",
                        "Private prosecution integration list",
                        "Private Court",
                        0,
                        0,
                        3);
        long entryId =
                insertApplicationListEntryRow(
                        listId,
                        applicationCodeId("MX99010"),
                        applicantId,
                        respondentId,
                        "Private {wording}",
                        "Private notes",
                        listDate);
        long highResolutionCodeId = resolutionCodeId("CASE");
        long lowResolutionCodeId = resolutionCodeId("AUTH");
        insertApplicationListEntryResolution(entryId, highResolutionCodeId);
        insertApplicationListEntryResolution(entryId, lowResolutionCodeId);
    }

    private void insertPrivateProsecutorsIndexStandardApplicantApplication(LocalDate listDate) {
        long standardApplicantId = standardApplicantId("APP001");
        long respondentId =
                insertNameAddressRow(
                        "Standard Respondent Ltd", null, null, null, "Respondent Street");
        long listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD998",
                        "Standard Private Hall",
                        "Private prosecution standard applicant integration list",
                        "Standard Private Court",
                        0,
                        0,
                        3);
        long entryId =
                insertStandardApplicantApplicationListEntryRow(
                        listId,
                        applicationCodeId("MX99010"),
                        standardApplicantId,
                        respondentId,
                        "Standard private {wording}",
                        "Standard private notes",
                        listDate);
        long resolutionCodeId = resolutionCodeId("REF");
        insertApplicationListEntryResolution(entryId, resolutionCodeId);
    }

    private long standardApplicantId(String code) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                      SELECT sa_id
                      FROM %s.standard_applicants
                      WHERE standard_applicant_code = ?
                      ORDER BY sa_id DESC
                      LIMIT 1
                      """,
                        schema),
                Long.class,
                code);
    }

    private void insertPrivateProsecutorsIndexIndividualStandardApplicantApplication(
            LocalDate listDate) {
        long standardApplicantId = standardApplicantId("APP002");
        long respondentId =
                insertNameAddressRow(
                        "Individual Standard Respondent Ltd", "", "", "", "Respondent Street");
        long listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD997",
                        "Individual Standard Private Hall",
                        "Private prosecution individual standard applicant integration list",
                        "Individual Standard Private Court",
                        0,
                        0,
                        3);
        long entryId =
                insertStandardApplicantApplicationListEntryRow(
                        listId,
                        applicationCodeId("MX99010"),
                        standardApplicantId,
                        respondentId,
                        "Individual standard private {wording}",
                        "Individual standard private notes",
                        listDate);
        long resolutionCodeId = resolutionCodeId("WDN");
        insertApplicationListEntryResolution(entryId, resolutionCodeId);
    }

    private void insertFeesReportApplication(
            LocalDate listDate,
            String applicantOrganisation,
            String applicantForename,
            String applicantSurname,
            String wording) {
        insertFeesReportApplication(
                listDate,
                applicantOrganisation,
                applicantForename,
                applicantSurname,
                wording,
                "Fees Hall");
    }

    private void insertFeesReportApplication(
            LocalDate listDate,
            String applicantOrganisation,
            String applicantForename,
            String applicantSurname,
            String wording,
            String otherCourthouse) {
        long applicantId =
                insertNameAddressRow(
                        applicantOrganisation,
                        applicantForename,
                        null,
                        applicantSurname,
                        "Fees Street");
        long listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD997",
                        otherCourthouse,
                        "Fees report integration list",
                        "Fees Court",
                        0,
                        0,
                        3);
        long entryId =
                insertApplicationListEntryRow(
                        listId,
                        applicationCodeId("AD99001"),
                        applicantId,
                        applicantId,
                        wording,
                        "Fees notes",
                        listDate);
        insertApplicationListEntryFee(entryId, feeId("CO5.1a"));
    }

    private long feeId(String feeReference) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                      SELECT fee_id
                      FROM %s.fee
                      WHERE fee_reference = ?
                      ORDER BY fee_start_date DESC, fee_id DESC
                      LIMIT 1
                      """,
                        schema),
                Long.class,
                feeReference);
    }

    private void insertApplicationListEntryFee(long entryId, long feeId) {
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.app_list_entry_fee_id (
                        ale_ale_id,
                        fee_fee_id,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        ?,
                        ?,
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    """,
                        schema),
                entryId,
                feeId);
    }

    private long insertNameAddressRow(
            String name,
            String firstName,
            String middleName,
            String lastName,
            String addressLine1) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.name_address (
                        na_id,
                        name,
                        first_name,
                        middle_name,
                        last_name,
                        address_l1,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        nextval('%s.na_seq'),
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    RETURNING na_id
                    """,
                        schema, schema),
                Long.class,
                name,
                firstName,
                middleName,
                lastName,
                addressLine1);
    }

    private long insertStandardApplicantRow(String name) {
        return insertStandardApplicantRow(name, null, null);
    }

    private long insertStandardApplicantRow(String name, String firstName, String surname) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.standard_applicants (
                        sa_id,
                        standard_applicant_code,
                        standard_applicant_start_date,
                        version,
                        changed_by,
                        changed_date,
                        user_name,
                        name,
                        forename_1,
                        surname,
                        address_l1
                    )
                    VALUES (
                        nextval('%s.sa_seq'),
                        ?,
                        DATE '2020-01-01',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test',
                        ?,
                        ?,
                        ?,
                        'Standard applicant street'
                    )
                    RETURNING sa_id
                    """,
                        schema, schema),
                Long.class,
                "STD" + Math.floorMod(System.nanoTime(), 1_000_000L),
                name,
                firstName,
                surname);
    }

    private long applicationCodeId(String applicationCode) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                      SELECT ac_id
                      FROM %s.application_codes
                      WHERE application_code = ?
                      ORDER BY ac_id DESC
                      LIMIT 1
                      """,
                        schema),
                Long.class,
                applicationCode);
    }

    private long resolutionCodeId(String resolutionCode) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                      SELECT rc_id
                      FROM %s.resolution_codes
                      WHERE resolution_code = ?
                      ORDER BY rc_id DESC
                      LIMIT 1
                      """,
                        schema),
                Long.class,
                resolutionCode);
    }

    private void insertApplicationListEntryResolution(long entryId, long resolutionCodeId) {
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.app_list_entry_resolutions (
                        aler_id,
                        rc_rc_id,
                        ale_ale_id,
                        al_entry_resolution_wording,
                        al_entry_resolution_officer,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        nextval('%s.aler_seq'),
                        ?,
                        ?,
                        'Resolution wording',
                        'Resolution officer',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    """,
                        schema, schema),
                resolutionCodeId,
                entryId);
    }

    private long insertApplicationList(
            LocalDate listDate,
            String courtCode,
            String description,
            String courtName,
            String isDeleted) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_lists (
                    al_id, application_list_status, application_list_date, application_list_time,
                    courthouse_code, list_description, version, changed_by, changed_date,
                    user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id, is_deleted
                ) VALUES (
                    nextval('%s.al_seq'), 'CLOSED', ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP,
                    'report-integration-test', ?, 0, 0, 3, ?
                )
                RETURNING al_id
                """
                        .formatted(schema, schema),
                Long.class,
                listDate,
                listDate.atTime(10, 0),
                courtCode,
                description,
                courtName,
                isDeleted);
    }

    private long insertNameAddress(
            String name, String firstName, String middleName, String lastName, String address) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.name_address (
                    na_id, name, first_name, middle_name, last_name, address_l1, version, changed_by,
                    changed_date, user_name
                ) VALUES (nextval('%s.na_seq'), ?, ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP, 'report-integration-test')
                RETURNING na_id
                """
                        .formatted(schema, schema),
                Long.class,
                name,
                firstName,
                middleName,
                lastName,
                address);
    }

    private void insertResolutionCodesAndResult(long entryId, String resultCode) {
        long resultId = resolutionCodeId(resultCode);
        jdbcTemplate.update(
                """
              INSERT INTO %s.app_list_entry_resolutions (
                  aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording,
                  al_entry_resolution_officer, version, changed_by, changed_date, user_name
              ) VALUES (
                  nextval('%s.aler_seq'), ?, ?, 'Resolution wording', 'Resolution officer',
                  1, 0, CURRENT_TIMESTAMP, 'report-integration-test'
              )
              """
                        .formatted(schema, schema),
                resultId,
                entryId);
    }

    private void insertOfficial(
            long entryId, String type, String title, String forename, String surname) {
        jdbcTemplate.update(
                """
                INSERT INTO %s.app_list_entry_official (
                    aleo_id, ale_ale_id, title, forename, surname, official_type, changed_by,
                    changed_date, user_name
                ) VALUES (
                    nextval('%s.aleo_seq'), ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, 'report-integration-test'
                )
                """
                        .formatted(schema, schema),
                entryId,
                title,
                forename,
                surname,
                type);
    }

    private void assertReportParameterAuditRow(
            ReportAuditOperation operation, String columnName, String value) {
        DataAudit persistedAuditRow =
                reportAuditRows(operation).stream()
                        .filter(row -> columnName.equals(row.getColumnName()))
                        .filter(row -> value.equals(row.getNewValue()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected a %s report parameter audit row for %s"
                                                        .formatted(
                                                                operation.getEventName(),
                                                                columnName)));

        Assertions.assertEquals("", persistedAuditRow.getOldValue());
        Assertions.assertEquals(operation.getType(), persistedAuditRow.getUpdateType());
        Assertions.assertEquals("report_parameters", persistedAuditRow.getTableName());
    }

    private void assertOnlyReportParametersAuditedFor(ReportAuditOperation operation) {
        Assertions.assertTrue(
                reportAuditRows(operation).stream()
                        .map(DataAudit::getTableName)
                        .allMatch(
                                tableName ->
                                        "report_parameters".equals(tableName)
                                                || "report_jobs".equals(tableName)));
    }

    private long insertApplicationListEntryRow(
            long listId,
            long applicationCodeId,
            long applicantId,
            long respondentId,
            String wording,
            String notes,
            LocalDate lodgementDate) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.application_list_entries (
                        ale_id,
                        al_al_id,
                        ac_ac_id,
                        a_na_id,
                        r_na_id,
                        application_list_entry_wording,
                        entry_rescheduled,
                        notes,
                        version,
                        changed_by,
                        changed_date,
                        user_name,
                        sequence_number,
                        lodgement_date
                    )
                    VALUES (
                        nextval('%s.ale_seq'),
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        'N',
                        ?,
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test',
                        1,
                        ?
                    )
                    RETURNING ale_id
                    """,
                        schema, schema),
                Long.class,
                listId,
                applicationCodeId,
                applicantId,
                respondentId,
                wording,
                notes,
                lodgementDate.atStartOfDay());
    }

    private long insertStandardApplicantApplicationListEntryRow(
            long listId,
            long applicationCodeId,
            long standardApplicantId,
            long respondentId,
            String wording,
            String notes,
            LocalDate lodgementDate) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.application_list_entries (
                        ale_id,
                        al_al_id,
                        ac_ac_id,
                        sa_sa_id,
                        r_na_id,
                        application_list_entry_wording,
                        entry_rescheduled,
                        notes,
                        version,
                        changed_by,
                        changed_date,
                        user_name,
                        sequence_number,
                        lodgement_date
                    )
                    VALUES (
                        nextval('%s.ale_seq'),
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        'N',
                        ?,
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test',
                        1,
                        ?
                    )
                    RETURNING ale_id
                    """,
                        schema, schema),
                Long.class,
                listId,
                applicationCodeId,
                standardApplicantId,
                respondentId,
                wording,
                notes,
                lodgementDate.atStartOfDay());
    }

    private long insertApplicationListEntry(
            long listId,
            long applicationCodeId,
            long applicantId,
            long respondentId,
            LocalDate date) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_list_entries (
                    ale_id, al_al_id, ac_ac_id, a_na_id, r_na_id, application_list_entry_wording,
                    entry_rescheduled, notes, version, changed_by, changed_date, user_name,
                    sequence_number, lodgement_date
                ) VALUES (
                    nextval('%s.ale_seq'), ?, ?, ?, ?, 'Workload wording', 'N', 'Workload notes',
                    1, 0, CURRENT_TIMESTAMP, 'report-integration-test', 1, ?
                )
                RETURNING ale_id
                """
                        .formatted(schema, schema),
                Long.class,
                listId,
                applicationCodeId,
                applicantId,
                respondentId,
                date.atStartOfDay());
    }

    private long insertEntry(
            LocalDate listDate,
            long appListId,
            String applicantName,
            int sequenceNumber,
            String isDeleted) {
        val applicantId = insertNameAddress(applicantName, null, null, null, "Applicant Street");
        val respondentId =
                insertNameAddress(
                        null, "Respondent " + applicantName, null, "Surname", "Respondent Street");
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_list_entries (
                    ale_id, al_al_id, ac_ac_id, a_na_id, r_na_id, application_list_entry_wording,
                    entry_rescheduled, notes, version, changed_by, changed_date, user_name,
                    sequence_number, lodgement_date, is_deleted
                ) VALUES (
                    nextval('%s.ale_seq'), ?, ?, ?, ?, 'Workload wording', 'N', 'Workload notes',
                    1, 0, CURRENT_TIMESTAMP, 'report-integration-test', ?, ?, ?
                )
                RETURNING ale_id
                """
                        .formatted(schema, schema),
                Long.class,
                appListId,
                applicationCodeId("MX99010"),
                applicantId,
                respondentId,
                sequenceNumber,
                listDate.atStartOfDay(),
                isDeleted);
    }

    private String createAndDownloadWorkloadReport(WorkloadFilterDto request) throws Exception {
        val token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        val createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT), token, request);
        createResponse.then().statusCode(202);

        val createdJob = createResponse.as(JobAcknowledgement.class);

        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    val jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);
                    return jobResponse.statusCode() == 200
                            && jobResponse.as(JobAcknowledgement.class).getStatus()
                                    == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        val downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);
        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (var responseStream = downloadResponse.getBody().asInputStream()) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String createAndDownloadActivityAuditReport(ActivityAuditFilterDto request)
            throws Exception {
        val token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        val createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(ACTIVITY_AUDIT_REPORT_WEB_CONTEXT), token, request);
        createResponse.then().statusCode(202);

        val createdJob = createResponse.as(JobAcknowledgement.class);

        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.ACTIVITY_AUDIT_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    val jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);
                    return jobResponse.statusCode() == 200
                            && jobResponse.as(JobAcknowledgement.class).getStatus()
                                    == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        val downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);
        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (var responseStream = downloadResponse.getBody().asInputStream()) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void createReportJobAndAwaitCompletion(
            String webContext, Object request, JobType expectedJobType) throws Exception {
        val token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        val createResponse =
                restAssuredClient.executePostRequest(getLocalUrl(webContext), token, request);
        createResponse.then().statusCode(202);

        val createdJob = createResponse.as(JobAcknowledgement.class);

        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(expectedJobType, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    val jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);
                    return jobResponse.statusCode() == 200
                            && jobResponse.as(JobAcknowledgement.class).getStatus()
                                    == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));
    }

    private String createAndDownloadListMaintenanceReport(ListMaintenanceFilterDto request)
            throws Exception {
        TokenAndJwksKey token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT), token, request);

        createResponse.then().statusCode(202);
        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.LIST_MAINTENANCE_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    return jobResponse.as(JobAcknowledgement.class).getStatus()
                            == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> workloadRow(String report, String applicantName) throws Exception {
        try (val reader = new CSVReader(new StringReader(report))) {
            val rows = reader.readAll();
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).length > 0 && "List Date".equals(rows.get(i)[0])) {
                    return firstMatchingRow(
                            rows.get(i), rows.subList(i + 1, rows.size()), applicantName);
                }
            }
        }
        throw new AssertionError("Workload CSV header row not found");
    }

    private Map<String, String> firstMatchingRow(
            String[] headers, List<String[]> rows, String applicantName) {
        for (val row : rows) {
            val values = valuesByHeader(headers, row);
            if (applicantName.equals(values.get("Applicant Name/Surname"))) {
                return values;
            }
        }
        throw new AssertionError("Workload row not found for " + applicantName);
    }

    private Map<String, String> valuesByHeader(String[] headers, String[] row) {
        val values = new HashMap<String, String>();
        for (int i = 0; i < headers.length; i++) {
            values.put(headers[i], i < row.length ? row[i] : "");
        }
        return values;
    }

    private Collection<DataAudit> reportAuditRows(ReportAuditOperation operation) {
        return dataAuditRepository.findAll().stream()
                .filter(row -> operation.getEventName().equals(row.getEventName()))
                .toList();
    }

    private String createFeesReportAndDownload(FeesReportFilterDto request) throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        TokenAndJwksKey token = tokenGenerator.fetchTokenForRole();

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT), token, request);

        createResponse.then().statusCode(202);
        JobAcknowledgement createdJob = createResponse.as(JobAcknowledgement.class);
        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.FEES_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
                () -> {
                    Response jobResponse =
                            restAssuredClient.executeGetRequest(
                                    getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())),
                                    token);

                    if (jobResponse.statusCode() != 200) {
                        return false;
                    }

                    JobAcknowledgement job = jobResponse.as(JobAcknowledgement.class);
                    Assertions.assertEquals(createdJob.getId(), job.getId());
                    Assertions.assertEquals(JobType.FEES_REPORT, job.getType());

                    return job.getStatus() == JobStatus1.COMPLETED;
                },
                Duration.ofSeconds(30));

        Response downloadResponse =
                restAssuredClient.executeGetRequest(
                        getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);

        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (InputStream responseStream = downloadResponse.getBody().asInputStream()) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
