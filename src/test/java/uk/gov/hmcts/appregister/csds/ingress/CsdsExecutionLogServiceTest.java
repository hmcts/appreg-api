package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class CsdsExecutionLogServiceTest {
    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T09:15:00Z"), ZoneId.of("UTC"));

    @Test
    void given_terminalStatusToday_when_hasTerminalStatusToday_then_returnsTrue() {
        var jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        var service = new CsdsExecutionLogService(jdbcTemplate, FIXED_CLOCK, UK_ZONE);
        ReflectionTestUtils.setField(service, "schema", "appreg");

        when(jdbcTemplate.queryForObject(
                        anyString(),
                        org.mockito.ArgumentMatchers.<Map<String, ?>>any(),
                        eq(Boolean.class)))
                .thenReturn(true);

        assertThat(service.hasTerminalStatusToday("CSDS_DATA_INGRESS")).isTrue();
    }

    @Test
    void given_messageLongerThanColumn_when_recordFailure_then_truncatesToColumnLength() {
        var jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        var service = new CsdsExecutionLogService(jdbcTemplate, FIXED_CLOCK, UK_ZONE);
        ReflectionTestUtils.setField(service, "schema", "appreg");
        var startedAt = LocalDateTime.of(2026, 7, 27, 10, 20);
        var longMessage = "x".repeat(1200);

        service.recordFailure("CSDS_DATA_INGRESS", startedAt, longMessage);

        verify(jdbcTemplate).update(anyString(), any(MapSqlParameterSource.class));
    }
}
