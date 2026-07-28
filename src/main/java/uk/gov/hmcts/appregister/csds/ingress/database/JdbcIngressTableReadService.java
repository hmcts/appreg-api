package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcIngressTableReadService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public <T> List<T> loadAll(String tableName, RowMapper<T> rowMapper) {
        CsdsSqlIdentifierValidator.requireValid(tableName, "tableName");
        val sql = "SELECT * FROM " + schema + "." + tableName;
        return jdbcTemplate.query(sql, rowMapper);
    }
}
