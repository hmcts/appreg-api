package uk.gov.hmcts.appregister.controller.reporting;

import com.opencsv.CSVReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.testutils.AwaitilityUtil;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;

public class ReportingControllerWorkloadOfficialsPostTest extends BaseIntegration {
    private static final String WORKLOAD_REPORT_WEB_CONTEXT = "reports/workload/jobs";
    private static final String JOB_WEB_CONTEXT = "jobs/%s";
    private static final String DOWNLOAD_WEB_CONTEXT = "reports/jobs/%s/download";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Test
    // Shows that magistrate columns should be based on official order, not list entry sequence.
    public void
            givenSingleMagistrateOnSecondListEntry_whenCreatingWorkloadReport_thenMagistrateAppearsInFirstColumn()
                    throws Exception {
        val listDate = LocalDate.of(2026, 9, 1); // fixes report date
        val applicantName = "Workload Second Entry Applicant"; // identifies target row
        val entryId = insertEntry(listDate, applicantName, 2); // creates second list entry
        insertOfficial(entryId, "M", "Solo", "Magistrate"); // adds one magistrate

        val row =
                workloadRow(
                        createAndDownloadWorkloadReport(listDate), applicantName); // reads CSV row

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
        val applicantName = "Workload Multiple Magistrates Applicant"; // identifies target row
        val entryId = insertEntry(listDate, applicantName, 1); // creates first list entry
        insertOfficial(entryId, "M", "First", "Magistrate"); // adds first magistrate
        insertOfficial(entryId, "M", "Second", "Magistrate"); // adds second magistrate

        val row =
                workloadRow(
                        createAndDownloadWorkloadReport(listDate), applicantName); // reads CSV row

        Assertions.assertEquals("Mr First Magistrate", row.get("JP1")); // expects first magistrate
        Assertions.assertEquals(
                "Mr Second Magistrate", row.get("JP2")); // expects second magistrate
        Assertions.assertEquals("", row.get("JP3")); // expects no third magistrate
    }

    private String createAndDownloadWorkloadReport(LocalDate listDate) throws Exception {
        val token =
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.ADMIN))
                        .build()
                        .fetchTokenForRole();
        val createResponse =
                restAssuredClient.executePostRequest(
                        getLocalUrl(WORKLOAD_REPORT_WEB_CONTEXT),
                        token,
                        new WorkloadFilterDto().dateFrom(listDate).dateTo(listDate));
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
        try (var stream = downloadResponse.getBody().asInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
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

    private long insertEntry(LocalDate listDate, String applicantName, int sequenceNumber) {
        val applicantId = insertNameAddress(applicantName);
        val respondentId = insertNameAddress("Respondent " + applicantName);
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
                insertApplicationList(listDate, applicantName),
                insertApplicationCode(),
                applicantId,
                respondentId,
                sequenceNumber,
                listDate.atStartOfDay());
    }

    private long insertApplicationList(LocalDate listDate, String description) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_lists (
                    al_id, application_list_status, application_list_date, application_list_time,
                    courthouse_code, list_description, version, changed_by, changed_date,
                    user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id
                ) VALUES (
                    nextval('%s.al_seq'), 'CLOSED', ?, ?, 'WLD002', ?, 1, 0, CURRENT_TIMESTAMP,
                    'report-integration-test', 'Workload Court', 0, 0, 3
                )
                RETURNING al_id
                """
                        .formatted(schema, schema),
                Long.class,
                listDate,
                listDate.atTime(10, 0),
                description);
    }

    private long insertApplicationCode() {
        val code = "WL" + Math.floorMod(System.nanoTime(), 1_000_000L);
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.application_codes (
                    ac_id, application_code, application_code_title, application_code_wording,
                    fee_due, application_code_respondent, application_code_start_date,
                    bulk_respondent_allowed, version, changed_by, changed_date, user_name
                ) VALUES (
                    nextval('%s.ac_seq'), ?, 'Workload Code', 'Workload wording', 'N', 'Y',
                    DATE '2020-01-01', 'N', 1, 0, CURRENT_TIMESTAMP, 'report-integration-test'
                )
                RETURNING ac_id
                """
                        .formatted(schema, schema),
                Long.class,
                code);
    }

    private long insertNameAddress(String name) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO %s.name_address (
                    na_id, name, address_l1, version, changed_by, changed_date, user_name
                ) VALUES (
                    nextval('%s.na_seq'), ?, 'Workload Street', 1, 0, CURRENT_TIMESTAMP,
                    'report-integration-test'
                )
                RETURNING na_id
                """
                        .formatted(schema, schema),
                Long.class,
                name);
    }

    private void insertOfficial(long entryId, String type, String forename, String surname) {
        jdbcTemplate.update(
                """
                INSERT INTO %s.app_list_entry_official (
                    aleo_id, ale_ale_id, title, forename, surname, official_type, changed_by,
                    changed_date, user_name
                ) VALUES (
                    nextval('%s.aleo_seq'), ?, 'Mr', ?, ?, ?, 0, CURRENT_TIMESTAMP,
                    'report-integration-test'
                )
                """
                        .formatted(schema, schema),
                entryId,
                forename,
                surname,
                type);
    }
}
