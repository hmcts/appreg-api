package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportDataReader implements DataReader<ActivityAuditReportRow> {
    private static final int MAX_ROWS = 1000;
    private static final String REPORT_QUERY =
            """
            WITH filtered_audit AS (
            SELECT
                %1$s AS activity_order,
                da.data_id,
                da.event_name,
                da.table_name,
                da.column_name,
                REPLACE(REPLACE(COALESCE(da.old_value, da.old_clob_value), '{', ''), '}', '')
                    AS old_value,
                REPLACE(REPLACE(COALESCE(da.new_value, da.new_clob_value), '{', ''), '}', '')
                    AS new_value,
                da.created_date::date AS created_date,
                da.created_date AS created_date_time,
                da.user_name
            FROM data_audit da
            WHERE da.created_date >= :dateFrom
                AND da.created_date < (:dateTo + INTERVAL '1 day')
                AND da.event_name IN (:eventNames)
                AND (
                    :username IS NULL
                    OR da.user_name = :username
                )
                AND da.column_name NOT LIKE '%%_ID%%'
            )
            SELECT
                activity_order,
                data_id,
                event_name,
                table_name,
                column_name,
                old_value,
                new_value,
                created_date,
                created_date_time,
                user_name
            FROM filtered_audit
            WHERE (
                :hasCursor IS FALSE
                OR activity_order > :lastActivityOrder
                OR (
                    activity_order = :lastActivityOrder
                    AND created_date_time > :lastCreatedDateTime
                )
                OR (
                    activity_order = :lastActivityOrder
                    AND created_date_time = :lastCreatedDateTime
                    AND data_id > :lastDataId
                )
            )
            ORDER BY
                activity_order,
                created_date_time,
                data_id
            LIMIT :limit
            """;

    private static final RowMapper<ActivityAuditReportRow> ROW_MAPPER =
            new ActivityAuditReportRowMapper();

    private static final Map<ActivityType, List<String>> EVENT_NAMES_BY_ACTIVITY =
            Map.ofEntries(
                    Map.entry(
                            ActivityType.ADD_APPLICATION,
                            List.of("Add Application", "Create Entry Application List")),
                    Map.entry(
                            ActivityType.ADD_STANDARD_APPLICANT,
                            List.of("Add Standard Applicant", "Create Applicant")),
                    Map.entry(
                            ActivityType.BULK_APPLICATION_UPLOAD,
                            List.of("Bulk Application Upload")),
                    Map.entry(
                            ActivityType.BULK_UPDATE_FEE_STATUS, List.of("Bulk Update Fee Status")),
                    Map.entry(ActivityType.BULK_UPDATE_OFFICIALS, List.of("Bulk Update Officials")),
                    Map.entry(
                            ActivityType.CREATE_APPLICATION_LIST,
                            List.of("Create Application List", "Add Application List")),
                    Map.entry(
                            ActivityType.DELETE_APPLICATION_ENTRY,
                            List.of("Delete Application Entry", "Delete Entry Application List")),
                    Map.entry(
                            ActivityType.DELETE_APPLICATION_LIST,
                            List.of("Delete Application List")),
                    Map.entry(
                            ActivityType.DELETE_RESULT_APPLICATION,
                            List.of(
                                    "Delete Result Application",
                                    "Delete Application List Entry Result")),
                    Map.entry(ActivityType.DELETE_RESULT_LIST, List.of("Delete Result List")),
                    Map.entry(
                            ActivityType.DELETE_RESULT_MULTIPLE_APPLICATIONS,
                            List.of("Delete Result Multiple Applications")),
                    Map.entry(
                            ActivityType.DELETE_STANDARD_APPLICANT,
                            List.of("Delete Standard Applicant", "Delete Applicant")),
                    Map.entry(
                            ActivityType.MOVE_APPLICATION,
                            List.of("Move Application", "Move Entry")),
                    Map.entry(
                            ActivityType.RESULT_APPLICATION,
                            List.of("Result Application", "Create Application List Entry Result")),
                    Map.entry(ActivityType.RESULT_LIST, List.of("Result List")),
                    Map.entry(
                            ActivityType.RESULT_MULTIPLE_APPLICATIONS,
                            List.of("Result Multiple Applications")),
                    Map.entry(
                            ActivityType.UPDATE_APPLICATION,
                            List.of("Update Application", "Update Entry Application List")),
                    Map.entry(
                            ActivityType.UPDATE_APPLICATION_LIST,
                            List.of("Update Application List")),
                    Map.entry(
                            ActivityType.UPDATE_RESULT_APPLICATION,
                            List.of(
                                    "Update Result Application",
                                    "Update Application List Entry Result")),
                    Map.entry(ActivityType.UPDATE_RESULT_LIST, List.of("Update Result List")),
                    Map.entry(
                            ActivityType.UPDATE_RESULT_MULTIPLE_APPLICATIONS,
                            List.of("Update Result Multiple Applications")),
                    Map.entry(
                            ActivityType.UPDATE_STANDARD_APPLICANT,
                            List.of("Update Standard Applicant")));

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ActivityAuditFilterDto filter;
    private final String schema;

    ActivityAuditReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate, ActivityAuditFilterDto filter, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<ActivityAuditReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        ActivityAuditReadCursor auditCursor = new ActivityAuditReadCursor(position.getPageSize());
        List<ActivityAuditReportRow> rows = readPage(auditCursor);

        while (!rows.isEmpty()) {
            pageReader.readData(rows, jobContext);
            auditCursor.advance(rows);
            rows = readPage(auditCursor);
        }
    }

    @Override
    public void close() throws IOException {
        // No stream to close.
    }

    private List<ActivityAuditReportRow> readPage(ActivityAuditReadCursor cursor) {
        if (cursor.isLimitReached()) {
            return List.of();
        }

        EventNameFilter eventNameFilter = eventNameFilter();
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue("username", filter.getUsername(), Types.VARCHAR)
                        .addValue("eventNames", eventNameFilter.eventNames())
                        .addValue("hasCursor", cursor.hasLastRow(), Types.BOOLEAN)
                        .addValue("lastActivityOrder", cursor.lastActivityOrder(), Types.INTEGER)
                        .addValue(
                                "lastCreatedDateTime",
                                cursor.lastCreatedDateTime(),
                                Types.TIMESTAMP)
                        .addValue("lastDataId", cursor.lastDataId(), Types.BIGINT)
                        .addValue("limit", cursor.pageSizeUpTo(MAX_ROWS), Types.INTEGER);

        for (int index = 0; index < eventNameFilter.orderedEventNames().size(); index++) {
            parameters.addValue(
                    "eventName" + index, eventNameFilter.orderedEventNames().get(index));
        }

        return jdbcTemplate.query(
                reportQuery(eventNameFilter.orderedEventNames()), parameters, ROW_MAPPER);
    }

    private EventNameFilter eventNameFilter() {
        List<String> eventNames = new ArrayList<>();

        for (ActivityType activityType : filter.getActivityTypes()) {
            List<String> names = EVENT_NAMES_BY_ACTIVITY.getOrDefault(activityType, List.of());
            for (String name : names) {
                if (!eventNames.contains(name)) {
                    eventNames.add(name);
                }
            }
        }

        return new EventNameFilter(eventNames, eventNames);
    }

    private String reportQuery(List<String> orderedEventNames) {
        return REPORT_QUERY.formatted(activityOrderExpression(orderedEventNames));
    }

    private String activityOrderExpression(List<String> orderedEventNames) {
        StringBuilder expression = new StringBuilder("CASE da.event_name");
        for (int index = 0; index < orderedEventNames.size(); index++) {
            expression.append(" WHEN :eventName").append(index).append(" THEN ").append(index);
        }
        expression.append(" ELSE ").append(orderedEventNames.size()).append(" END");
        return expression.toString();
    }

    private record EventNameFilter(List<String> eventNames, List<String> orderedEventNames) {}

    private static class ActivityAuditReadCursor {
        private final int pageSize;
        private int rowsRead;
        private ActivityAuditReportRow lastRow;

        ActivityAuditReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<ActivityAuditReportRow> rows) {
            rowsRead += rows.size();
            lastRow = rows.getLast();
        }

        boolean isLimitReached() {
            return rowsRead >= MAX_ROWS;
        }

        int pageSizeUpTo(int maximumRows) {
            return Math.min(pageSize, maximumRows - rowsRead);
        }

        boolean hasLastRow() {
            return lastRow != null;
        }

        Integer lastActivityOrder() {
            return hasLastRow() ? lastRow.getActivityOrder() : null;
        }

        LocalDateTime lastCreatedDateTime() {
            return hasLastRow() ? lastRow.getCreatedDateTime() : null;
        }

        Long lastDataId() {
            return hasLastRow() ? lastRow.getDataId() : null;
        }
    }

    private static class ActivityAuditReportRowMapper implements RowMapper<ActivityAuditReportRow> {
        @Override
        public ActivityAuditReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ActivityAuditReportRow.builder()
                    .dataId(rs.getLong("data_id"))
                    .activityOrder(rs.getInt("activity_order"))
                    .eventName(rs.getString("event_name"))
                    .tableName(rs.getString("table_name"))
                    .columnName(rs.getString("column_name"))
                    .oldValue(rs.getString("old_value"))
                    .newValue(rs.getString("new_value"))
                    .createdDate(rs.getObject("created_date", java.time.LocalDate.class))
                    .createdDateTime(
                            rs.getObject("created_date_time", java.time.LocalDateTime.class))
                    .userName(rs.getString("user_name"))
                    .build();
        }
    }
}
