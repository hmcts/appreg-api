package uk.gov.hmcts.appregister.csds.ingress.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings(
        "java:S2077") // Schema comes from trusted config; user values are parameter-bound.
public class CsdsAuditWriteService {
    private static final String INSERT_SQL =
            """
            INSERT INTO %s.csds_audit (
                ca_id,
                appreg_table_name,
                appreg_action,
                appreg_key,
                csds_json,
                error
            )
            VALUES (
                nextval('%s.ca_seq'),
                :appregTableName,
                :appregAction,
                :appregKey,
                :csdsJson,
                :error
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public void persist(List<CsdsAuditEntry> auditsToPersist) {
        if (auditsToPersist.isEmpty()) {
            return;
        }

        var trustedSchema = schema; // NOSONAR
        // S2077: schema is trusted Spring config and all runtime values are parameter-bound.
        var sql = INSERT_SQL.formatted(trustedSchema, trustedSchema);
        var parameters =
                auditsToPersist.stream()
                        .map(this::toParameters)
                        .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(sql, parameters);
    }

    private MapSqlParameterSource toParameters(CsdsAuditEntry item) {
        return new MapSqlParameterSource()
                .addValue("appregTableName", item.appregTableName())
                .addValue("appregAction", item.appregAction())
                .addValue("appregKey", item.appregKey())
                .addValue("csdsJson", item.csdsJson())
                .addValue("error", item.error());
    }
}
