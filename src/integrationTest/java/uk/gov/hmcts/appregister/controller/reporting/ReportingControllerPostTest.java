package uk.gov.hmcts.appregister.controller.reporting;

import com.opencsv.CSVReader;
import io.restassured.response.Response;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.appregister.common.entity.DataAudit;
import uk.gov.hmcts.appregister.common.entity.repository.DataAuditRepository;
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
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenAndJwksKey;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ReportingControllerPostTest extends BaseIntegration {
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

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private DataAuditRepository dataAuditRepository;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    public void
            givenValidActivityAuditReportRequest_whenCreatingReport_thenFilteredCsvCanBeDownloaded()
                    throws Exception {
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "STD_APPLICANT_CODE",
                "",
                "{AW62958}",
                OffsetDateTime.now(),
                "alice");
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_NAME",
                "",
                "Second page value",
                OffsetDateTime.now().plusSeconds(1),
                "alice");
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_ADDRESSLINE1",
                "",
                "Third page value",
                OffsetDateTime.now().plusSeconds(2),
                "alice");
        insertDataAuditRow(
                "Update Application",
                "APPLICATION_LIST_ENTRIES",
                "STD_APPLICANT_CODE",
                "AW62958",
                "AW62959",
                OffsetDateTime.now(),
                "alice");
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "RESP_NAME",
                "",
                "Hidden",
                OffsetDateTime.now(),
                "bob");
        insertDataAuditRow(
                "Bulk Application Upload",
                "APPLICATION_LIST_ENTRIES",
                "AL_ID",
                "",
                "12345",
                OffsetDateTime.now(),
                "alice");

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();
        LocalDate today = LocalDate.now();
        ActivityAuditFilterDto request =
                new ActivityAuditFilterDto()
                        .dateFrom(today)
                        .dateTo(today)
                        .username("alice")
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
                ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT, "username", "alice");
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
            Assertions.assertTrue(report.contains("Activity Audit Report"));
            Assertions.assertTrue(report.contains("Bulk Application Upload"));
            Assertions.assertTrue(report.contains("AW62958"));
            Assertions.assertTrue(report.contains("Second page value"));
            Assertions.assertTrue(report.contains("Third page value"));
            Assertions.assertFalse(report.contains("{AW62958}"));
            Assertions.assertFalse(report.contains("Update Application"));
            Assertions.assertFalse(report.contains("Hidden"));
            Assertions.assertFalse(report.contains("12345"));
        }
    }

    @Test
    public void
            givenValidFeesReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1))
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
            Assertions.assertTrue(report.contains("Fees Report"));
        }
    }

    @Test
    public void
            givenFeesReportApplicantNameMatchesPersonSurname_whenCreatingReport_thenCsvIncludesEntry()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, 5, 18);
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

        Assertions.assertTrue(report.contains("Fees Report"));
        Assertions.assertTrue(report.contains("ArcPerson Singlefield"));
        Assertions.assertFalse(report.contains("Arc Organisation Applicant Ltd"));
    }

    @Test
    public void
            givenFeesReportApplicantNameMatchesOrganisation_whenCreatingReport_thenCsvIncludesEntry()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, 5, 19);
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

        Assertions.assertTrue(report.contains("Fees Report"));
        Assertions.assertTrue(report.contains("Arc Organisation Singlefield Ltd"));
        Assertions.assertFalse(report.contains("ArcPerson Unmatched"));
    }

    @Test
    public void givenUnknownCjaCode_whenCreatingFeesReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void givenCourtAndCjaLocation_whenCreatingFeesReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
                                "Either 'courtLocation' must be provided, or both "
                                        + "'criminalJusticeArea' and 'otherLocationDescription' "
                                        + "must be supplied."));
    }

    @Test
    public void givenUnknownCourtCode_whenCreatingFeesReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(FEES_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(createResponse.asString().contains("Court not found"));
    }

    @Test
    public void givenDuplicateCjaCode_whenCreatingFeesReport_thenConflictIsReturned()
            throws Exception {
        insertDuplicateCjaRows("Z1");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void
            givenValidSearchWarrantsReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        SearchWarrantsReportFilterDto request =
                new SearchWarrantsReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1))
                        .dateFrom(LocalDate.of(2018, 5, 31))
                        .dateTo(LocalDate.of(2018, 5, 1))
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
            Assertions.assertTrue(report.contains("Search Warrants Report"));
        }
    }

    @Test
    public void
            givenValidDurationReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, 4, 10);
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
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
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
            Assertions.assertTrue(report.contains("Duration Report"));
            Assertions.assertTrue(report.contains("10/04/2026"));
            Assertions.assertTrue(report.contains("XCD123 - Duration Court"));
            Assertions.assertTrue(report.contains("County Hall"));
            Assertions.assertTrue(report.contains("Duration report integration list"));
            Assertions.assertTrue(report.contains("2"));
            Assertions.assertTrue(report.contains("45"));
        }
    }

    @Test
    public void givenUnknownCjaCode_whenCreatingDurationReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void givenUnknownCourtCode_whenCreatingDurationReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(DURATION_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(createResponse.asString().contains("Court not found"));
    }

    @Test
    public void givenDuplicateCjaCode_whenCreatingDurationReport_thenConflictIsReturned()
            throws Exception {
        insertDuplicateCjaRows("Z2");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        DurationFilterDto request =
                new DurationFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void
            givenValidWorkloadReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28));

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
            Assertions.assertTrue(report.contains("Workload Report"));
        }
    }

    @Test
    public void
            givenValidWorkloadReportWithOtherLocationFilter_whenCreatingReport_thenJobIsMadeAndReportCanBeDownloaded()
                    throws Exception {

        val listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        LocalDate.of(2026, 4, 15),
                        "TH",
                        "Town Hall",
                        "Workload Report - Other Location",
                        "Workload Court",
                        0,
                        0,
                        3);

        val entryId =
                insertEntry(LocalDate.of(2026, 4, 15), listId, "Workload Report Applicant", 1);
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
                        .dateFrom(LocalDate.of(2026, 4, 15))
                        .dateTo(LocalDate.of(2026, 4, 15))
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
            Assertions.assertTrue(report.contains("Workload Report"));
            Assertions.assertTrue(report.contains("Town Hall"));
            Assertions.assertTrue(report.contains("CD"));
        }
    }

    @Test
    public void
            givenValidWorkloadReportWithJustCJACode_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {

        val listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        LocalDate.of(2026, 4, 16),
                        "TH",
                        "Town Hall",
                        "Workload Report - CJA Code Only",
                        "Workload Court",
                        0,
                        0,
                        3);

        val entryId =
                insertEntry(LocalDate.of(2026, 4, 16), listId, "Workload Report Applicant", 1);
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
                        .dateFrom(LocalDate.of(2026, 4, 16))
                        .dateTo(LocalDate.of(2026, 4, 16))
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
            Assertions.assertTrue(report.contains("Workload Report"));
            Assertions.assertTrue(report.contains("Town Hall"));
            Assertions.assertTrue(report.contains("CD"));
        }
    }

    @Test
    public void
            givenOtherLocationProvidedMissingCJAFilter_whenCreatingWorkloadReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription("Some other location");
        location.setCjaCode(null);

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
    }

    @Test
    // Shows that magistrate columns should be based on official order, not list entry sequence.
    public void
            givenSingleMagistrateOnSecondListEntry_whenCreatingWorkloadReport_thenMagistrateAppearsInFirstColumn()
                    throws Exception {
        val listDate = LocalDate.of(2026, 9, 1); // fixes report date
        val applicantName = "Workload Second Entry Applicant"; // identifies target row
        val listId =
                insertApplicationList(
                        listDate, "WLD001", "Workload multi resolution list", "Workload Court");
        val entryId = insertEntry(listDate, listId, applicantName, 2); // creates second list entry
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
        val listDate = LocalDate.of(2026, 9, 2); // fixes report date
        val applicantName = "Workload Multiple Magistrates Applicant";
        val appListId =
                insertApplicationList(
                        listDate,
                        "WLD002",
                        "Workload Court",
                        applicantName); // creates list// identifies target row
        val entryId =
                insertApplicationListEntry(
                        appListId,
                        insertApplicationCodeRow(),
                        insertNameAddress(
                                "Workload Multiple Magistrates Applicant", null, null, "Test Road"),
                        insertNameAddress("", "Test", "Respondent", "Test Road"),
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
    public void
            givenCourtProvidedWithCJAFilter_whenCreatingWorkloadReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        LegacyReportLocation location = new LegacyReportLocation();
        location.setOtherLocationDescription(null);
        location.setCjaCode("01");
        location.setCourtLocationCode("TEST123");

        WorkloadFilterDto request =
                new WorkloadFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
    }

    @Test
    public void
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
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
                        .location(location);

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
    }

    @Test
    public void
            givenWorkloadEntryHasMultipleResolutionsAndOfficials_whenCreatingReport_thenSingleCsvRowContainsAllData()
                    throws Exception {
        val listDate = LocalDate.of(2026, 8, 17);
        val applicantName = "Workload Multi Resolution Applicant";
        val applicantId = insertNameAddress(applicantName, null, null, "Workload Street");
        val respondentId =
                insertNameAddress("Workload Respondent", null, null, "Respondent Street");
        val listId =
                insertApplicationList(
                        listDate, "WLD001", "Workload multi resolution list", "Workload Court");
        val entryId =
                insertApplicationListEntry(
                        listId,
                        applicationCodeIdOrInsert("MX99010"),
                        applicantId,
                        respondentId,
                        listDate);

        insertResolutionCodesAndResult(entryId, "WRA");
        insertResolutionCodesAndResult(entryId, "WRB");
        // Resolutions and officials are both one-to-many joins; the report should still emit
        // one workload row per application list entry.
        insertOfficial(entryId, "M", "Mr", "Jill", "Magistrate");
        insertOfficial(entryId, "C", "Mr", "Casey", "Clerk");

        val report =
                createAndDownloadWorkloadReport(
                        new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate));
        val applicantRows = report.lines().filter(line -> line.contains(applicantName)).toList();

        Assertions.assertEquals(1, applicantRows.size()); // asserts one row per entry
        Assertions.assertTrue(applicantRows.getFirst().contains("WRA")); // includes first result
        Assertions.assertTrue(applicantRows.getFirst().contains("WRB")); // includes second result
        Assertions.assertTrue(applicantRows.getFirst().contains("Jill Magistrate")); // includes JP
        Assertions.assertTrue(
                applicantRows.getFirst().contains("Casey Clerk")); // includes official
    }

    @Test
    public void givenValidPrivateProsecutorsIndexRequest_whenCreatingReport_thenCsvCanBeDownloaded()
            throws Exception {
        LocalDate listDate = LocalDate.of(2026, 4, 11);
        insertPrivateProsecutorsIndexApplication(listDate);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
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
            Assertions.assertTrue(report.contains("Private Prosecution Index Report"));
            Assertions.assertTrue(report.contains("11/04/2026"));
            Assertions.assertTrue(report.contains("XCD999 - Private Court"));
            Assertions.assertTrue(report.contains("Private Hall"));
            Assertions.assertTrue(report.contains("CD"));
            Assertions.assertTrue(report.contains("Legacy"));
            Assertions.assertTrue(report.contains("Private"));
            Assertions.assertTrue(report.contains("Respondent Org Ltd"));
            Assertions.assertTrue(report.contains("Private wording"));
            Assertions.assertTrue(report.contains("PIZ"));
            Assertions.assertTrue(report.contains("PIA"));
            Assertions.assertTrue(report.contains("Private notes"));
            Assertions.assertFalse(report.contains("{wording}"));
        }
    }

    @Test
    public void
            givenValidPrivateProsecutorsIndexRequestForStandardApplicant_whenCreatingReport_thenCsvCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, 4, 12);
        insertPrivateProsecutorsIndexStandardApplicantApplication(listDate);

        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2026, 4, 1))
                        .dateTo(LocalDate.of(2026, 4, 28))
                        .standardApplicantName("Standards")
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
                "Standards");
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
            Assertions.assertTrue(report.contains("Private Prosecution Index Report"));
            Assertions.assertTrue(report.contains("12/04/2026"));
            Assertions.assertTrue(report.contains("XCD998 - Standard Private Court"));
            Assertions.assertTrue(report.contains("Private Standards Body"));
            Assertions.assertTrue(report.contains("Standard Respondent Ltd"));
            Assertions.assertTrue(report.contains("Standard private wording"));
            Assertions.assertTrue(report.contains("PIS"));
            Assertions.assertTrue(report.contains("Standard private notes"));
            Assertions.assertTrue(report.contains("CD,,,Private Standards Body,"));
        }
    }

    @Test
    public void
            givenWhitespaceOnlyPrivateProsecutorsIndexFilters_whenCreatingReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .applicantSurname(" ")
                        .respondentFirstName(" ")
                        .location(new LegacyReportLocation().cjaCode(" "));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(PRIVATE_PROSECUTORS_INDEX_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(createResponse.asString().contains("Validation failed for fields:"));
        Assertions.assertTrue(createResponse.asString().contains("applicantSurname"));
        Assertions.assertTrue(createResponse.asString().contains("respondentFirstName"));
        Assertions.assertTrue(createResponse.asString().contains("location.cjaCode"));
    }

    @Test
    public void
            givenPrivateProsecutorsIndexFilterContainsInternalWhitespace_whenCreatingReport_thenCsvCanBeDownloaded()
                    throws Exception {
        TokenAndJwksKey token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();

        PrivateProsecutorsIndexFilterDto request =
                new PrivateProsecutorsIndexFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
            Assertions.assertTrue(report.contains("Private Prosecution Index Report"));
            Assertions.assertEquals(2, report.lines().count());
        }
    }

    @Test
    public void
            givenValidListMaintenanceReportRequest_whenCreatingReport_thenJobIsCreatedAndReportCanBeDownloaded()
                    throws Exception {
        LocalDate listDate = LocalDate.of(2026, 4, 11);
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
                        .dateFrom(LocalDate.of(2026, 4, 30))
                        .dateTo(LocalDate.of(2026, 4, 1))
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
            Assertions.assertTrue(report.contains("List Maintenance Report"));
            Assertions.assertTrue(
                    report.contains(
                            "List Date,List Court House Name,List Other Location,CJA Code,"
                                    + "List Description,List Status,No Of Application Entries"));
            Assertions.assertTrue(report.contains("11/04/2026"));
            Assertions.assertTrue(report.contains("XCD123 - Maintenance Court"));
            Assertions.assertTrue(report.contains("County Hall"));
            Assertions.assertTrue(report.contains("CD"));
            Assertions.assertTrue(report.contains("List maintenance integration,OPEN,2"));
            Assertions.assertFalse(report.contains("Closed maintenance list"));
            Assertions.assertFalse(report.contains("Unmatched report list"));
        }
    }

    @Test
    public void
            givenListMaintenanceRowsOnDateBounds_whenNoLocationFilter_thenBothBoundsAreIncluded()
                    throws Exception {
        String description = "LM boundary report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 1),
                "AAA001",
                null,
                description,
                "Boundary Start Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 30),
                "AAA002",
                null,
                description,
                "Boundary End Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 7, 1),
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
                                .dateFrom(LocalDate.of(2026, 6, 1))
                                .dateTo(LocalDate.of(2026, 6, 30))
                                .listDescription(description));

        Assertions.assertTrue(report.contains("01/06/2026,AAA001 - Boundary Start Court"));
        Assertions.assertTrue(report.contains("30/06/2026,AAA002 - Boundary End Court"));
        Assertions.assertFalse(report.contains("Outside Boundary Court"));
    }

    @Test
    public void givenCourtFilter_whenCreatingListMaintenanceReport_thenOnlyMatchingCourtIsReturned()
            throws Exception {
        String description = "LM court filter report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 15),
                "CCC003",
                null,
                description,
                "Cardiff Crown Court",
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 15),
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
                                .dateFrom(LocalDate.of(2026, 6, 1))
                                .dateTo(LocalDate.of(2026, 6, 30))
                                .listDescription(description)
                                .location(new LegacyReportLocation().courtLocationCode("CCC003")));

        Assertions.assertTrue(report.contains("CCC003 - Cardiff Crown Court"));
        Assertions.assertFalse(report.contains("BCC006 - Bristol Crown Court"));
    }

    @Test
    public void
            givenOtherLocationAndCjaFilter_whenCreatingListMaintenanceReport_thenOnlyMatchingRowIsReturned()
                    throws Exception {
        String description = "LM other location report";
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 15),
                null,
                "Village Hall",
                description,
                null,
                0,
                0,
                3);
        insertApplicationListRow(
                "OPEN",
                LocalDate.of(2026, 6, 15),
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
                                .dateFrom(LocalDate.of(2026, 6, 1))
                                .dateTo(LocalDate.of(2026, 6, 30))
                                .listDescription(description)
                                .location(
                                        new LegacyReportLocation()
                                                .cjaCode("CD")
                                                .otherLocationDescription("village")));

        Assertions.assertTrue(report.contains("Village Hall,CD," + description));
        Assertions.assertFalse(report.contains("Village Hall,CE," + description));
    }

    @Test
    public void givenNoMatchingRows_whenCreatingListMaintenanceReport_thenEmptyCsvIsReturned()
            throws Exception {
        String report =
                createAndDownloadListMaintenanceReport(
                        new ListMaintenanceFilterDto()
                                .dateFrom(LocalDate.of(2026, 6, 1))
                                .dateTo(LocalDate.of(2026, 6, 30))
                                .listDescription("LM no matching report rows"));

        Assertions.assertTrue(report.contains("List Maintenance Report"));
        Assertions.assertTrue(
                report.contains(
                        "List Date,List Court House Name,List Other Location,CJA Code,"
                                + "List Description,List Status,No Of Application Entries"));
        Assertions.assertEquals(2, report.lines().count());
    }

    @Test
    public void givenUnknownCjaCode_whenCreatingListMaintenanceReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void givenUnknownCourtCode_whenCreatingListMaintenanceReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(new LegacyReportLocation().courtLocationCode("ZZ999"));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(createResponse.asString().contains("Court not found"));
    }

    @Test
    public void givenDuplicateCjaCode_whenCreatingListMaintenanceReport_thenConflictIsReturned()
            throws Exception {
        insertDuplicateCjaRows("Z3");
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
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
    public void
            givenWhitespaceOnlyListMaintenanceFilters_whenCreatingReport_thenBadRequestIsReturned()
                    throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        ListMaintenanceFilterDto request =
                new ListMaintenanceFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .listDescription(" ")
                        .location(new LegacyReportLocation().cjaCode(" "));

        Response createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(LIST_MAINTENANCE_REPORT_WEB_CONTEXT),
                        tokenGenerator.fetchTokenForRole(),
                        request);

        createResponse.then().statusCode(400);
        Assertions.assertTrue(createResponse.asString().contains("Validation failed for fields:"));
        Assertions.assertTrue(createResponse.asString().contains("listDescription"));
        Assertions.assertTrue(createResponse.asString().contains("location.cjaCode"));
    }

    private void insertDataAuditRow(
            String eventName,
            String tableName,
            String columnName,
            String oldValue,
            String newValue,
            OffsetDateTime createdDate,
            String userName) {
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
                userName,
                createdDate,
                eventName,
                userName);
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
        Long applicationCodeId = insertApplicationCodeRow();
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

    private Long insertApplicationCodeRow() {
        Long applicationCodeId =
                jdbcTemplate.queryForObject(
                        String.format("SELECT nextval('%s.ac_seq')", schema), Long.class);
        String applicationCode = "LM" + Math.floorMod(applicationCodeId, 100_000_000);
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.application_codes (
                        ac_id,
                        application_code,
                        application_code_title,
                        application_code_wording,
                        fee_due,
                        application_code_respondent,
                        application_code_start_date,
                        bulk_respondent_allowed,
                        version,
                        changed_by,
                        changed_date
                    )
                    VALUES (
                        ?,
                        ?,
                        'List Maintenance Code',
                        'List Maintenance Wording',
                        'N',
                        'N',
                        CURRENT_TIMESTAMP,
                        'N',
                        1,
                        0,
                        CURRENT_TIMESTAMP
                    )
                    """,
                        schema),
                applicationCodeId,
                applicationCode);
        return applicationCodeId;
    }

    private void insertPrivateProsecutorsIndexApplication(LocalDate listDate) {
        long applicantId = insertNameAddressRow(null, "Private", "Legacy", "Applicant Street");
        long respondentId =
                insertNameAddressRow("Respondent Org Ltd", null, null, "Respondent Street");
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
                        applicationCodeIdOrInsert("MX99010"),
                        applicantId,
                        respondentId,
                        "Private {wording}",
                        "Private notes",
                        listDate);
        long highResolutionCodeId = insertResolutionCode("PIZ");
        long lowResolutionCodeId = insertResolutionCode("PIA");
        insertApplicationListEntryResolution(entryId, highResolutionCodeId);
        insertApplicationListEntryResolution(entryId, lowResolutionCodeId);
    }

    private void insertPrivateProsecutorsIndexStandardApplicantApplication(LocalDate listDate) {
        long standardApplicantId = insertStandardApplicantRow("Private Standards Body");
        long respondentId =
                insertNameAddressRow("Standard Respondent Ltd", null, null, "Respondent Street");
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
                        applicationCodeIdOrInsert("MX99010"),
                        standardApplicantId,
                        respondentId,
                        "Standard private {wording}",
                        "Standard private notes",
                        listDate);
        long resolutionCodeId = insertResolutionCode("PIS");
        insertApplicationListEntryResolution(entryId, resolutionCodeId);
    }

    private void insertFeesReportApplication(
            LocalDate listDate,
            String applicantOrganisation,
            String applicantForename,
            String applicantSurname,
            String wording) {
        long applicantId =
                insertNameAddressRow(
                        applicantOrganisation, applicantForename, applicantSurname, "Fees Street");
        long listId =
                insertApplicationListRowReturningId(
                        "CLOSED",
                        listDate,
                        "XCD997",
                        "Fees Hall",
                        "Fees report integration list",
                        "Fees Court",
                        0,
                        0,
                        3);
        long entryId =
                insertApplicationListEntryRow(
                        listId,
                        insertFeesApplicationCodeRow(),
                        applicantId,
                        applicantId,
                        wording,
                        "Fees notes",
                        listDate);
        long feeId = insertFeeRow();
        insertApplicationListEntryFee(entryId, feeId);
    }

    private long insertFeesApplicationCodeRow() {
        Long applicationCodeId =
                jdbcTemplate.queryForObject(
                        String.format("SELECT nextval('%s.ac_seq')", schema), Long.class);
        String applicationCode = "FR" + Math.floorMod(applicationCodeId, 1_000_000L);
        jdbcTemplate.update(
                String.format(
                        """
                    INSERT INTO %s.application_codes (
                        ac_id,
                        application_code,
                        application_code_title,
                        application_code_wording,
                        fee_due,
                        application_code_respondent,
                        application_code_start_date,
                        bulk_respondent_allowed,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        ?,
                        ?,
                        'Fees Report Code',
                        'Fees report wording',
                        'Y',
                        'N',
                        DATE '2020-01-01',
                        'N',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    """,
                        schema),
                applicationCodeId,
                applicationCode);
        return applicationCodeId;
    }

    private long insertFeeRow() {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.fee (
                        fee_id,
                        fee_reference,
                        fee_description,
                        fee_value,
                        fee_start_date,
                        fee_version,
                        fee_changed_by,
                        fee_changed_date,
                        fee_user_name,
                        is_offsite
                    )
                    VALUES (
                        nextval('%s.fee_seq'),
                        ?,
                        'Fees report integration fee',
                        10.00,
                        DATE '2020-01-01',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test',
                        false
                    )
                    RETURNING fee_id
                    """,
                        schema, schema),
                Long.class,
                "FR" + Math.floorMod(System.nanoTime(), 1_000_000_000L));
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
            String name, String firstName, String surname, String addressLine1) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.name_address (
                        na_id,
                        name,
                        forename_1,
                        surname,
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
                surname,
                addressLine1);
    }

    private long insertStandardApplicantRow(String name) {
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
                        'Standard applicant street'
                    )
                    RETURNING sa_id
                    """,
                        schema, schema),
                Long.class,
                "STD" + Math.floorMod(System.nanoTime(), 1_000_000L),
                name);
    }

    private long applicationCodeIdOrInsert(String applicationCode) {
        List<Long> applicationCodeIds =
                jdbcTemplate.queryForList(
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
        if (!applicationCodeIds.isEmpty()) {
            return applicationCodeIds.getFirst();
        }

        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.application_codes (
                        ac_id,
                        application_code,
                        application_code_title,
                        application_code_wording,
                        application_legislation,
                        fee_due,
                        application_code_respondent,
                        application_code_start_date,
                        bulk_respondent_allowed,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        nextval('%s.ac_seq'),
                        ?,
                        'Application for a private prosecution summons',
                        'Application for private prosecution {TEXT|Summarise offence title(s)|250}',
                        'Section 1 Magistrates Courts Act 1980',
                        'N',
                        'Y',
                        DATE '2020-01-01',
                        'N',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    RETURNING ac_id
                    """,
                        schema, schema),
                Long.class,
                applicationCode);
    }

    private long insertResolutionCode(String resolutionCode) {
        return jdbcTemplate.queryForObject(
                String.format(
                        """
                    INSERT INTO %s.resolution_codes (
                        rc_id,
                        resolution_code,
                        resolution_code_title,
                        resolution_code_wording,
                        resolution_code_start_date,
                        version,
                        changed_by,
                        changed_date,
                        user_name
                    )
                    VALUES (
                        nextval('%s.rc_seq'),
                        ?,
                        ?,
                        ?,
                        DATE '2020-01-01',
                        1,
                        0,
                        CURRENT_TIMESTAMP,
                        'report-integration-test'
                    )
                    RETURNING rc_id
                    """,
                        schema, schema),
                Long.class,
                resolutionCode,
                resolutionCode + " title",
                resolutionCode + " wording");
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
            LocalDate listDate, String courtCode, String description, String courtName) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_lists (
                    al_id, application_list_status, application_list_date, application_list_time,
                    courthouse_code, list_description, version, changed_by, changed_date,
                    user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id
                ) VALUES (
                    nextval('%s.al_seq'), 'CLOSED', ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP,
                    'report-integration-test', ?, 0, 0, 3
                )
                RETURNING al_id
                """
                        .formatted(schema, schema),
                Long.class,
                listDate,
                listDate.atTime(10, 0),
                courtCode,
                description,
                courtName);
    }

    private long insertNameAddress(String name, String firstName, String surname, String address) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.name_address (
                    na_id, name, forename_1, surname, address_l1, version, changed_by,
                    changed_date, user_name
                ) VALUES (nextval('%s.na_seq'), ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP, 'report-integration-test')
                RETURNING na_id
                """
                        .formatted(schema, schema),
                Long.class,
                name,
                firstName,
                surname,
                address);
    }

    private void insertResolutionCodesAndResult(long entryId, String resultCode) {
        val resultId =
                jdbcTemplate.queryForObject(
                        """
                    INSERT INTO %s.resolution_codes (
                        rc_id, resolution_code, resolution_code_title, resolution_code_wording,
                        resolution_code_start_date, version, changed_by, changed_date, user_name
                    ) VALUES (
                        nextval('%s.rc_seq'), ?, ?, ?, DATE '2020-01-01', 1, 0,
                        CURRENT_TIMESTAMP, 'report-integration-test'
                    )
                    RETURNING rc_id
                    """
                                .formatted(schema, schema),
                        Long.class,
                        resultCode,
                        resultCode + " title",
                        resultCode + " wording");
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
            LocalDate listDate, long appListId, String applicantName, int sequenceNumber) {
        val applicantId = insertNameAddress(applicantName, null, null, "Applicant Street");
        val respondentId =
                insertNameAddress(
                        null, "Respondent " + applicantName, "Surname", "Respondent Street");
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_list_entries (
                    ale_id, al_al_id, ac_ac_id, a_na_id, r_na_id, application_list_entry_wording,
                    entry_rescheduled, notes, version, changed_by, changed_date, user_name,
                    sequence_number, lodgement_date
                ) VALUES (
                    nextval('%s.ale_seq'), ?, ?, ?, ?, 'Workload wording', 'N', 'Workload notes',
                    1, 0, CURRENT_TIMESTAMP, 'report-integration-test', ?, ?
                )
                RETURNING ale_id
                """
                        .formatted(schema, schema),
                Long.class,
                appListId,
                insertApplicationCodeRow(),
                applicantId,
                respondentId,
                sequenceNumber,
                listDate.atStartOfDay());
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
