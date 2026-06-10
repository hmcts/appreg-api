package uk.gov.hmcts.appregister.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportDataReaderTest {
    @Test
    void givenReportRowsExist_whenReadData_thenReadsPagesWithExpectedParameters() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        JobContext jobContext = mock(JobContext.class);
        List<List<ActivityAuditReportRow>> pages = new ArrayList<>();
        PageReader<ActivityAuditReportRow> pageReader =
                (rows, context) -> {
                    Assertions.assertSame(jobContext, context);
                    pages.add(rows);
                };
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();
        List<String> queries = new ArrayList<>();
        AtomicInteger queryCount = new AtomicInteger();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<ActivityAuditReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            queries.add(invocation.getArgument(0));
                            parameterSources.add(invocation.getArgument(1));
                            RowMapper<ActivityAuditReportRow> rowMapper = invocation.getArgument(2);
                            if (queryCount.getAndIncrement() == 0) {
                                return List.of(rowMapper.mapRow(resultSet(), 0));
                            }
                            return List.of();
                        });

        ActivityAuditReportDataReader reader =
                new ActivityAuditReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(new ReadPagePosition(1, 5), pageReader, jobContext);

        verify(rawJdbcTemplate).execute("SET LOCAL search_path TO \"appreg\"");
        Assertions.assertEquals(1, pages.size());
        ActivityAuditReportRow row = pages.getFirst().getFirst();

        Assertions.assertEquals(123L, row.getDataId());
        Assertions.assertEquals(0, row.getActivityOrder());
        Assertions.assertEquals("Add Application", row.getEventName());
        Assertions.assertEquals("APPLICATION_LIST_ENTRY", row.getTableName());
        Assertions.assertEquals("APPLICATION_CODE", row.getColumnName());
        Assertions.assertEquals("old", row.getOldValue());
        Assertions.assertEquals("new", row.getNewValue());
        Assertions.assertEquals(LocalDate.of(2026, Month.APRIL, 1), row.getCreatedDate());
        Assertions.assertEquals(
                LocalDateTime.of(2026, Month.APRIL, 1, 10, 15), row.getCreatedDateTime());
        Assertions.assertEquals("caseworker@example.com", row.getUserName());

        Assertions.assertEquals(2, parameterSources.size());
        assertParameters(parameterSources.getFirst(), false);
        assertParameters(parameterSources.get(1), true);
        assertQueryShape(queries.getFirst());
    }

    @Test
    void givenLargePageSize_whenReadData_thenUsesPageSizeForEachPage() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTemplate rawJdbcTemplate = mock(JdbcTemplate.class);
        List<MapSqlParameterSource> parameterSources = new ArrayList<>();

        when(jdbcTemplate.getJdbcTemplate()).thenReturn(rawJdbcTemplate);
        when(jdbcTemplate.query(
                        anyString(),
                        any(MapSqlParameterSource.class),
                        ArgumentMatchers.<RowMapper<ActivityAuditReportRow>>any()))
                .thenAnswer(
                        invocation -> {
                            parameterSources.add(invocation.getArgument(1));
                            return rows(20);
                        });

        ActivityAuditReportDataReader reader =
                new ActivityAuditReportDataReader(jdbcTemplate, filter(), "appreg");

        reader.readData(
                new ReadPagePosition(600, 0),
                (rows, context) -> assertThat(rows).isNotEmpty(),
                mock(JobContext.class));

        Assertions.assertEquals(1, parameterSources.size());
        Assertions.assertEquals(600, parameterSources.getFirst().getValue("limit"));
    }

    @Test
    void givenAuditColumnNames_whenCheckingIdColumnPredicate_thenOnlyLiteralIdSuffixIsExcluded() {
        Assertions.assertFalse(wouldPassIdColumnPredicate("al_id"));
        Assertions.assertFalse(wouldPassIdColumnPredicate("AL_ID"));
        Assertions.assertTrue(wouldPassIdColumnPredicate("PAID"));
        Assertions.assertTrue(wouldPassIdColumnPredicate("application_code"));
    }

    private void assertParameters(MapSqlParameterSource parameters, boolean expectedCursor) {
        Assertions.assertEquals(
                LocalDate.of(2026, Month.APRIL, 1), parameters.getValue("dateFrom"));
        Assertions.assertEquals(LocalDate.of(2026, Month.APRIL, 30), parameters.getValue("dateTo"));
        Assertions.assertEquals("caseworker@example.com", parameters.getValue("username"));
        Assertions.assertEquals(
                List.of(
                        "Add Application",
                        "Create Entry Application List",
                        "Update Application",
                        "Update Entry Application List",
                        "Create Activity Audit Report",
                        "Create Fees Report",
                        "Create Duration Report",
                        "Create List Maintenance Report",
                        "Create Private Prosecutors Index Report",
                        "Report Job Status Transition",
                        "Download Report"),
                parameters.getValue("eventNames"));
        Assertions.assertEquals(1, parameters.getValue("limit"));
        Assertions.assertEquals(expectedCursor, parameters.getValue("hasCursor"));
        Assertions.assertFalse(parameters.hasValue("lastActivityOrder"));
        Assertions.assertEquals("Add Application", parameters.getValue("eventName0"));
        Assertions.assertEquals("Create Entry Application List", parameters.getValue("eventName1"));
        Assertions.assertEquals("Update Application", parameters.getValue("eventName2"));
        Assertions.assertEquals("Update Entry Application List", parameters.getValue("eventName3"));
        Assertions.assertEquals("Create Activity Audit Report", parameters.getValue("eventName4"));
        Assertions.assertEquals("Create Fees Report", parameters.getValue("eventName5"));
        Assertions.assertEquals("Create Duration Report", parameters.getValue("eventName6"));
        Assertions.assertEquals(
                "Create List Maintenance Report", parameters.getValue("eventName7"));
        Assertions.assertEquals(
                "Create Private Prosecutors Index Report", parameters.getValue("eventName8"));
        Assertions.assertEquals("Report Job Status Transition", parameters.getValue("eventName9"));
        Assertions.assertEquals("Download Report", parameters.getValue("eventName10"));

        if (expectedCursor) {
            Assertions.assertEquals(
                    LocalDateTime.of(2026, Month.APRIL, 1, 10, 15),
                    parameters.getValue("lastCreatedDateTime"));
            Assertions.assertEquals(123L, parameters.getValue("lastDataId"));
        } else {
            Assertions.assertNull(parameters.getValue("lastCreatedDateTime"));
            Assertions.assertNull(parameters.getValue("lastDataId"));
        }
    }

    private void assertQueryShape(String query) {
        Assertions.assertTrue(
                query.contains(
                        "CASE da.event_name WHEN :eventName0 THEN 0 WHEN :eventName1 THEN 1"));
        assertThat(query).contains("da.created_date >= :dateFrom");
        assertThat(query).contains("da.event_name IN (:eventNames)");
        Assertions.assertTrue(
                query.contains("COALESCE(NULLIF(da.user_id, ''), da.user_name) AS user_name"));
        Assertions.assertTrue(
                query.contains("OR COALESCE(NULLIF(da.user_id, ''), da.user_name) = :username"));
        assertThat(query).contains("POSITION('_ID' IN UPPER(da.column_name)) = 0");
        Assertions.assertTrue(
                query.contains("Maintains legacy MIS Activity Audit report ordering"));
        assertThat(query).contains("ORDER BY");
        Assertions.assertEquals(-1, query.indexOf(":lastActivityOrder"));
        Assertions.assertEquals(-1, query.indexOf("ORDER BY\n                activity_order"));
        assertThat(query.replaceAll("\\s+", " ")).contains("ORDER BY created_date_time, data_id");
        assertThat(query).contains("LIMIT :limit");
    }

    private boolean wouldPassIdColumnPredicate(String columnName) {
        return !columnName.toUpperCase().contains("_ID");
    }

    private ActivityAuditFilterDto filter() {
        return new ActivityAuditFilterDto()
                .dateFrom(LocalDate.of(2026, Month.APRIL, 1))
                .dateTo(LocalDate.of(2026, Month.APRIL, 30))
                .username("caseworker@example.com")
                .activityTypes(
                        List.of(
                                ActivityType.ADD_APPLICATION,
                                ActivityType.UPDATE_APPLICATION,
                                ActivityType.REPORT_CREATED,
                                ActivityType.REPORT_STATUS_TRANSITION,
                                ActivityType.REPORT_DOWNLOADED,
                                ActivityType.ADD_APPLICATION));
    }

    private List<ActivityAuditReportRow> rows(int size) {
        List<ActivityAuditReportRow> rows = new ArrayList<>();
        for (long index = 0; index < size; index++) {
            rows.add(
                    ActivityAuditReportRow.builder()
                            .dataId(index)
                            .activityOrder(0)
                            .createdDateTime(LocalDateTime.of(2026, Month.APRIL, 1, 10, 15))
                            .build());
        }
        return rows;
    }

    private ResultSet resultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("data_id")).thenReturn(123L);
        when(resultSet.getInt("activity_order")).thenReturn(0);
        when(resultSet.getString("event_name")).thenReturn("Add Application");
        when(resultSet.getString("table_name")).thenReturn("APPLICATION_LIST_ENTRY");
        when(resultSet.getString("column_name")).thenReturn("APPLICATION_CODE");
        when(resultSet.getString("old_value")).thenReturn("old");
        when(resultSet.getString("new_value")).thenReturn("new");
        when(resultSet.getObject("created_date", LocalDate.class))
                .thenReturn(LocalDate.of(2026, Month.APRIL, 1));
        when(resultSet.getObject("created_date_time", LocalDateTime.class))
                .thenReturn(LocalDateTime.of(2026, Month.APRIL, 1, 10, 15));
        when(resultSet.getString("user_name")).thenReturn("caseworker@example.com");
        return resultSet;
    }
}
