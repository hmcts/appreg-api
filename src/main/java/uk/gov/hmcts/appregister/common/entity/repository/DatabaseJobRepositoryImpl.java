package uk.gov.hmcts.appregister.common.entity.repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.entity.TableNames;

@RequiredArgsConstructor
public class DatabaseJobRepositoryImpl implements DatabaseJobLockRepositoryCustom {
    private static final String JOB_NAME_PARAMETER = "jobName";
    private static final String TOKEN_PARAMETER = "token";

    private static final String ACQUIRE_LEASE_SQL =
            """
            UPDATE %s
               SET job_metadata = :token,
                   job_last_ran = current_timestamp
             WHERE job_name = :jobName
               AND job_enabled = 'Y'
               AND (
                    job_metadata IS NULL
                    OR job_last_ran IS NULL
                    OR job_last_ran < current_timestamp - (:leaseMillis * interval '1 millisecond')
               )
            RETURNING job_metadata
            """;

    private static final String RENEW_LEASE_SQL =
            """
            UPDATE %s
               SET job_last_ran = current_timestamp
             WHERE job_name = :jobName
               AND job_enabled = 'Y'
               AND job_metadata = :token
               AND job_last_ran >= current_timestamp - (:leaseMillis * interval '1 millisecond')
            RETURNING job_metadata
            """;

    private static final String RELEASE_LEASE_SQL =
            """
            UPDATE %s
               SET job_metadata = NULL,
                   job_last_ran = NULL
             WHERE job_name = :jobName
               AND job_metadata = :token
            RETURNING job_name
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Override
    public Optional<String> tryAcquireLease(String jobName, String token, Duration leaseDuration) {
        return queryForFirstValue(
                ACQUIRE_LEASE_SQL,
                Map.of(
                        JOB_NAME_PARAMETER,
                        jobName,
                        TOKEN_PARAMETER,
                        token,
                        "leaseMillis",
                        leaseDuration.toMillis()));
    }

    @Override
    public boolean renewLease(String jobName, String token, Duration leaseDuration) {
        return queryForFirstValue(
                        RENEW_LEASE_SQL,
                        Map.of(
                                JOB_NAME_PARAMETER,
                                jobName,
                                TOKEN_PARAMETER,
                                token,
                                "leaseMillis",
                                leaseDuration.toMillis()))
                .isPresent();
    }

    @Override
    public boolean releaseLease(String jobName, String token) {
        return queryForFirstValue(
                        RELEASE_LEASE_SQL,
                        Map.of(JOB_NAME_PARAMETER, jobName, TOKEN_PARAMETER, token))
                .isPresent();
    }

    private Optional<String> queryForFirstValue(String sql, Map<String, Object> parameters) {
        return jdbcTemplate.query(
                sql.formatted(schema + "." + TableNames.DATABASE_JOBS), // NOSONAR
                // S2077: schema is trusted Spring config and all runtime values are
                // parameter-bound.
                new MapSqlParameterSource(parameters),
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty());
    }
}
