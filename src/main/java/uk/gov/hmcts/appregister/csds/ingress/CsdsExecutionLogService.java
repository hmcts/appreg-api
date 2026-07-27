package uk.gov.hmcts.appregister.csds.ingress;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@Component
@RequiredArgsConstructor
@SuppressWarnings(
        "java:S2077") // Schema name is trusted config; runtime values are parameter-bound.
public class CsdsExecutionLogService {
    static final String STATUS_SUCCESS = "SUCCESS";
    static final String STATUS_FAILED = "FAILED";

    private static final int MESSAGE_MAX_LENGTH = 1000;
    private static final String HAS_TERMINAL_STATUS_SQL =
            """
            SELECT COUNT(*) > 0
            FROM %s
            WHERE job_name = :jobName
              AND execution_status IN (:successStatus, :failedStatus)
              AND execution_start_time >= :dayStart
              AND execution_start_time < :nextDayStart
            """;
    private static final String INSERT_STATUS_SQL =
            """
            INSERT INTO %s (
                djel_id,
                job_name,
                execution_start_time,
                execution_end_time,
                execution_status,
                execution_message
            ) VALUES (
                nextval('%s'),
                :jobName,
                :executionStartTime,
                :executionEndTime,
                :executionStatus,
                :executionMessage
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ZoneId ukZone;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public boolean hasTerminalStatusToday(String jobName) {
        var dayStart = LocalDateTime.now(clock.withZone(ukZone)).toLocalDate().atStartOfDay();
        var nextDayStart = dayStart.plusDays(1);
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        HAS_TERMINAL_STATUS_SQL.formatted(
                                schema + "." + TableNames.DATABASE_JOB_EXECUTION_LOG),
                        Map.of(
                                "jobName",
                                jobName,
                                "successStatus",
                                STATUS_SUCCESS,
                                "failedStatus",
                                STATUS_FAILED,
                                "dayStart",
                                dayStart,
                                "nextDayStart",
                                nextDayStart),
                        Boolean.class));
    }

    public void recordSuccess(String jobName, LocalDateTime executionStartTime, String message) {
        insertStatus(jobName, executionStartTime, STATUS_SUCCESS, message);
    }

    public void recordFailure(String jobName, LocalDateTime executionStartTime, String message) {
        insertStatus(jobName, executionStartTime, STATUS_FAILED, message);
    }

    private void insertStatus(
            String jobName,
            LocalDateTime executionStartTime,
            String executionStatus,
            String message) {
        var executionLogTable = schema + "." + TableNames.DATABASE_JOB_EXECUTION_LOG;
        var legacySequence = schema + ".djel_seq";
        jdbcTemplate.update(
                INSERT_STATUS_SQL.formatted(executionLogTable, legacySequence),
                new MapSqlParameterSource()
                        .addValue("jobName", jobName)
                        .addValue("executionStartTime", executionStartTime)
                        .addValue("executionEndTime", LocalDateTime.now(clock.withZone(ukZone)))
                        .addValue("executionStatus", executionStatus)
                        .addValue("executionMessage", truncate(message)));
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX_LENGTH);
    }
}
