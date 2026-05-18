package uk.gov.hmcts.appregister.controller.reporting;

import lombok.val;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

public class ReportingControllerWorkloadPostTest extends BaseIntegration {
    private static final String WORKLOAD_REPORT_WEB_CONTEXT = "reports/workload/jobs";
    private static final String JOB_WEB_CONTEXT = "jobs/%s";
    private static final String DOWNLOAD_WEB_CONTEXT = "reports/jobs/%s/download";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    public void givenWorkloadEntryHasMultipleResolutionsAndOfficials_whenCreatingReport_thenSingleCsvRowContainsAllData() throws Exception {
        val listDate = LocalDate.of(2026, 8, 17);
        val applicantName = "Workload Multi Resolution Applicant";
        val applicantId = insertNameAddress(applicantName, null, null, "Workload Street");
        val respondentId = insertNameAddress("Workload Respondent", null, null, "Respondent Street");
        val listId = insertApplicationList(
            listDate,
            "WLD001",
            "Workload multi resolution list",
            "Workload Court"
        );
        val entryId = insertApplicationListEntry(
            listId,
            applicationCodeIdOrInsert("MX99010"),
            applicantId,
            respondentId,
            listDate
        );

        insertResult(entryId, "WRA");
        insertResult(entryId, "WRB");
        // Resolutions and officials are both one-to-many joins; the report should still emit
        // one workload row per application list entry.
        insertOfficial(entryId, "M", "Jill", "Magistrate");
        insertOfficial(entryId, "C", "Casey", "Clerk");

        val report = createAndDownloadWorkloadReport(new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate));
        val applicantRows = report.lines().filter(line -> line.contains(applicantName)).toList();

        Assertions.assertEquals(1, applicantRows.size()); // asserts one row per entry
        Assertions.assertTrue(applicantRows.getFirst().contains("WRA")); // includes first result
        Assertions.assertTrue(applicantRows.getFirst().contains("WRB")); // includes second result
        Assertions.assertTrue(applicantRows.getFirst().contains("Jill Magistrate")); // includes JP
        Assertions.assertTrue(applicantRows.getFirst().contains("Casey Clerk")); // includes official
    }

    private String createAndDownloadWorkloadReport(WorkloadFilterDto request) throws Exception {
        val token = getATokenWithValidCredentials()
            .roles(List.of(RoleEnum.ADMIN))
            .build()
            .fetchTokenForRole();
        val createResponse = restAssuredClient.executePostRequest(getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT), token, request);
        createResponse.then().statusCode(202);

        val createdJob = createResponse.as(JobAcknowledgement.class);

        Assertions.assertNotNull(createdJob.getId());
        Assertions.assertEquals(JobType.WORKLOAD_REPORT, createdJob.getType());

        AwaitilityUtil.waitForMaxWithOneSecondPoll(
            () -> {
                val jobResponse = restAssuredClient.executeGetRequest(getLocalUrl(JOB_WEB_CONTEXT.formatted(createdJob.getId())), token);
                return jobResponse.statusCode() == 200 && jobResponse.as(JobAcknowledgement.class).getStatus() == JobStatus1.COMPLETED;
            },
            Duration.ofSeconds(30));

        val downloadResponse = restAssuredClient.executeGetRequest(getLocalUrl(DOWNLOAD_WEB_CONTEXT.formatted(createdJob.getId())), token);
        downloadResponse.then().statusCode(200);
        downloadResponse.then().contentType("text/csv");
        try (var responseStream = downloadResponse.getBody().asInputStream()) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private long insertApplicationList(LocalDate listDate, String courtCode, String description, String courtName) {
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
            """.formatted(schema, schema),
            Long.class,
            listDate,
            listDate.atTime(10, 0),
            courtCode,
            description,
            courtName
        );
    }

    private long insertNameAddress(String name, String firstName, String surname, String address) {
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO %s.name_address (
                na_id, name, forename_1, surname, address_l1, version, changed_by,
                changed_date, user_name
            ) VALUES (nextval('%s.na_seq'), ?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP, 'report-integration-test')
            RETURNING na_id
            """.formatted(schema, schema),
            Long.class,
            name,
            firstName,
            surname,
            address
        );
    }

    private long insertApplicationListEntry(long listId, long applicationCodeId, long applicantId, long respondentId, LocalDate date) {
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
            """.formatted(schema, schema),
            Long.class,
            listId,
            applicationCodeId,
            applicantId,
            respondentId,
            date.atStartOfDay()
        );
    }

    private long applicationCodeIdOrInsert(String applicationCode) {
        val ids = jdbcTemplate.queryForList(
            "SELECT ac_id FROM %s.application_codes WHERE application_code = ? ORDER BY ac_id DESC LIMIT 1".formatted(schema),
            Long.class,
            applicationCode
        );
        if (!ids.isEmpty()) {
            return ids.getFirst();
        }
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO %s.application_codes (
                ac_id, application_code, application_code_title, application_code_wording,
                application_legislation, fee_due, application_code_respondent,
                application_code_start_date, bulk_respondent_allowed, version, changed_by,
                changed_date, user_name
            ) VALUES (
                nextval('%s.ac_seq'), ?, 'Application for a private prosecution summons',
                'Application for private prosecution {TEXT|Summarise offence title(s)|250}',
                'Section 1 Magistrates Courts Act 1980', 'N', 'Y', DATE '2020-01-01',
                'N', 1, 0, CURRENT_TIMESTAMP, 'report-integration-test'
            )
            RETURNING ac_id
            """.formatted(schema, schema),
            Long.class,
            applicationCode
        );
    }

    private void insertResult(long entryId, String resultCode) {
        val resultId = jdbcTemplate.queryForObject(
            """
            INSERT INTO %s.resolution_codes (
                rc_id, resolution_code, resolution_code_title, resolution_code_wording,
                resolution_code_start_date, version, changed_by, changed_date, user_name
            ) VALUES (
                nextval('%s.rc_seq'), ?, ?, ?, DATE '2020-01-01', 1, 0,
                CURRENT_TIMESTAMP, 'report-integration-test'
            )
            RETURNING rc_id
            """.formatted(schema, schema),
            Long.class,
            resultCode,
            resultCode + " title",
            resultCode + " wording"
        );
        jdbcTemplate.update(
            """
            INSERT INTO %s.app_list_entry_resolutions (
                aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording,
                al_entry_resolution_officer, version, changed_by, changed_date, user_name
            ) VALUES (
                nextval('%s.aler_seq'), ?, ?, 'Resolution wording', 'Resolution officer',
                1, 0, CURRENT_TIMESTAMP, 'report-integration-test'
            )
            """.formatted(schema, schema),
            resultId,
            entryId
        );
    }

    private void insertOfficial(long entryId, String type, String forename, String surname) {
        jdbcTemplate.update(
            """
            INSERT INTO %s.app_list_entry_official (
                aleo_id, ale_ale_id, forename, surname, official_type, changed_by,
                changed_date, user_name
            ) VALUES (
                nextval('%s.aleo_seq'), ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, 'report-integration-test'
            )
            """.formatted(schema, schema),
            entryId,
            forename,
            surname,
            type
        );
    }
}
