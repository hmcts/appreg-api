package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JdbcIngressTableReadServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private RowMapper<Object> rowMapper;

    @InjectMocks private JdbcIngressTableReadService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "schema", "appreg");
    }

    @Test
    @SuppressWarnings("unchecked")
    void given_tableName_when_loadAll_then_queryExpectedTable() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn((List) List.of("row"));

        var result = service.loadAll("application_codes_staging", rowMapper);

        assertThat(result).containsExactly("row");
        verify(jdbcTemplate).query("SELECT * FROM appreg.application_codes_staging", rowMapper);
    }

    @Test
    void given_invalidTableName_when_loadAll_then_rejectIt() {
        assertThatThrownBy(() -> service.loadAll("application-codes-test", rowMapper))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL tableName");
    }
}
