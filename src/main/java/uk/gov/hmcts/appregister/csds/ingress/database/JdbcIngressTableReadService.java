package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JdbcIngressTableReadService {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public <T> List<T> loadAll(String tableName, RowMapper<T> rowMapper) {
        validateIdentifier(tableName, "tableName");
        val sql = "SELECT * FROM " + schema + "." + tableName;
        return jdbcTemplate.query(sql, rowMapper);
    }

    private void validateIdentifier(String value, String description) {
        if (!StringUtils.hasText(value) || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL " + description + ": " + value);
        }
    }
}
