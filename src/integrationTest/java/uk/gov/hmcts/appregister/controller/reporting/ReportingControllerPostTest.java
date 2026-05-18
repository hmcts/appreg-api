package uk.gov.hmcts.appregister.controller.reporting;

import io.restassured.response.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
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
    private static final String LIST_MAINTENANCE_REPORT_WEB_CONTEXT =
            "reports/list-maintenance/jobs";
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
                        .location(
                                new LegacyReportLocation()
                                        .courtLocationCode("CCC003")
                                        .otherLocationDescription("Town Hall")
                                        .cjaCode("CD"));

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
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "Town Hall");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "cjaCode", "CD");
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
    public void givenUnknownCjaCode_whenCreatingFeesReport_thenBadRequestIsReturned()
            throws Exception {
        TokenGenerator tokenGenerator =
                getATokenWithValidCredentials().roles(List.of(RoleEnum.ADMIN)).build();

        FeesReportFilterDto request =
                new FeesReportFilterDto()
                        .dateFrom(LocalDate.of(2018, 5, 1))
                        .dateTo(LocalDate.of(2018, 5, 31))
                        .location(new LegacyReportLocation().cjaCode("QX"));

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
                        .location(new LegacyReportLocation().cjaCode("Z1"));

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
                        .location(
                                new LegacyReportLocation()
                                        .courtLocationCode("CCC003")
                                        .otherLocationDescription("Town Hall")
                                        .cjaCode("CD"));
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
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "Town Hall");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT, "cjaCode", "CD");
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
                        .location(new LegacyReportLocation().cjaCode("CD"));

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
                        .location(new LegacyReportLocation().cjaCode("QX"));

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
                        .location(new LegacyReportLocation().cjaCode("Z2"));

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
                        .location(new LegacyReportLocation().cjaCode("CD"));

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
                        .location(new LegacyReportLocation().cjaCode("QX"));

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
                        .location(new LegacyReportLocation().cjaCode("Z3"));

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

    private Collection<DataAudit> reportAuditRows(ReportAuditOperation operation) {
        return dataAuditRepository.findAll().stream()
                .filter(row -> operation.getEventName().equals(row.getEventName()))
                .toList();
    }
}
