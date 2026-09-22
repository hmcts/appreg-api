package uk.gov.hmcts.appregister.report.service;

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
import uk.gov.hmcts.appregister.generated.model.ActivityAuditFilterDto;
import uk.gov.hmcts.appregister.generated.model.ActivityType;
import uk.gov.hmcts.appregister.report.audit.ReportAuditOperation;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportDataReader
        extends AbstractReportDataReader<
                ActivityAuditReportRow, ActivityAuditReportDataReader.ActivityAuditReadCursor> {
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
                COALESCE(NULLIF(da.user_name, ''), da.user_id) AS user_name
            FROM data_audit da
            WHERE da.created_date >= :dateFrom
                AND da.created_date < (:dateTo + INTERVAL '1 day')
                AND da.event_name IN (:eventNames)
                AND (
                    :username IS NULL
                    OR COALESCE(NULLIF(da.user_name, ''), da.user_id) = :username
                )
                AND POSITION('_ID' IN UPPER(da.column_name)) = 0
                -- data_id is a deterministic tie-breaker so keyset paging cannot skip or duplicate rows.
                AND (
                    :hasCursor IS FALSE
                    OR (da.created_date, da.data_id) < (:lastCreatedDateTime, :lastDataId)
                )
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
            -- Show the most recent audit activity first so configured row caps retain useful data.
            ORDER BY
                created_date_time DESC,
                data_id DESC
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
                            ActivityType.MOVE_APPLICATION,
                            List.of("Move Application", "Move Entry")),
                    Map.entry(
                            ActivityType.REPORT_CREATED,
                            List.of(
                                    ReportAuditOperation.CREATE_ACTIVITY_AUDIT_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation.CREATE_FEES_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation.CREATE_SEARCH_WARRANTS_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation.CREATE_DURATION_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation.CREATE_WORKLOAD_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation.CREATE_LIST_MAINTENANCE_REPORT_AUDIT_EVENT
                                            .getEventName(),
                                    ReportAuditOperation
                                            .CREATE_PRIVATE_PROSECUTORS_INDEX_REPORT_AUDIT_EVENT
                                            .getEventName())),
                    Map.entry(
                            ActivityType.REPORT_DOWNLOADED,
                            List.of(
                                    ReportAuditOperation.DOWNLOAD_REPORT_AUDIT_EVENT
                                            .getEventName())),
                    Map.entry(
                            ActivityType.REPORT_STATUS_TRANSITION,
                            List.of(
                                    ReportAuditOperation.REPORT_JOB_STATUS_TRANSITION_AUDIT_EVENT
                                            .getEventName())),
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
                            List.of("Update Result Multiple Applications")));

    private final ActivityAuditFilterDto filter;

    ActivityAuditReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            ActivityAuditFilterDto filter,
            String schema,
            int maxRows) {
        super(jdbcTemplate, schema, maxRows);
        this.filter = filter;
    }

    ActivityAuditFilterDto filter() {
        return filter;
    }

    @Override
    protected ActivityAuditReadCursor createCursor(int pageSize) {
        return new ActivityAuditReadCursor(pageSize);
    }

    @Override
    protected List<ActivityAuditReportRow> readPage(ActivityAuditReadCursor cursor, int pageLimit) {
        EventNameFilter eventNameFilter = eventNameFilter();
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue("username", filter.getUsername(), Types.VARCHAR)
                        .addValue("eventNames", eventNameFilter.eventNames())
                        .addValue("hasCursor", cursor.hasLastRow(), Types.BOOLEAN)
                        .addValue(
                                "lastCreatedDateTime",
                                cursor.lastCreatedDateTime(),
                                Types.TIMESTAMP)
                        .addValue("lastDataId", cursor.lastDataId(), Types.BIGINT)
                        .addValue("limit", pageLimit, Types.INTEGER);

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
        StringBuilder expression = new StringBuilder(50).append("CASE da.event_name");
        for (int index = 0; index < orderedEventNames.size(); index++) {
            expression.append(" WHEN :eventName").append(index).append(" THEN ").append(index);
        }
        expression.append(" ELSE ").append(orderedEventNames.size()).append(" END");
        return expression.toString();
    }

    private record EventNameFilter(List<String> eventNames, List<String> orderedEventNames) {}

    static class ActivityAuditReadCursor extends ReportReadCursor<ActivityAuditReportRow> {
        ActivityAuditReadCursor(int pageSize) {
            super(pageSize);
        }

        LocalDateTime lastCreatedDateTime() {
            return hasLastRow() ? lastRow().getCreatedDateTime() : null;
        }

        Long lastDataId() {
            return hasLastRow() ? lastRow().getDataId() : null;
        }
    }

    static class ActivityAuditReportRowMapper implements RowMapper<ActivityAuditReportRow> {
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
