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
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.Location;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;

public class ReportingControllerPostTest extends BaseIntegration {
    private static final String FEES_REPORT_WEB_CONTEXT = "reports/fees/jobs";
    private static final String ACTIVITY_AUDIT_REPORT_WEB_CONTEXT = "reports/activity-audit/jobs";
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
                                new Location()
                                        .courtLocationCode("LOC123")
                                        .otherLocationDescription("Town Hall")
                                        .cjaCode("52"));

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
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "courtLocationCode", "LOC123");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT,
                "otherLocationDescription",
                "Town Hall");
        assertReportParameterAuditRow(
                ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT, "cjaCode", "52");
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
                        .allMatch("report_parameters"::equals));
    }

    private Collection<DataAudit> reportAuditRows(ReportAuditOperation operation) {
        return dataAuditRepository.findAll().stream()
                .filter(row -> operation.getEventName().equals(row.getEventName()))
                .toList();
    }
}
