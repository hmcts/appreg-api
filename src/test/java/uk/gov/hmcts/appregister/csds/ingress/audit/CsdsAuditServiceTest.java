package uk.gov.hmcts.appregister.csds.ingress.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
class CsdsAuditServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private CsdsAuditWriteService csdsAuditWriteService;
    @Mock private CsdsAuditFailurePersistenceService csdsAuditFailurePersistenceService;

    private CsdsAuditService service;

    @BeforeEach
    void setUp() {
        service =
                new CsdsAuditService(
                        jdbcTemplate, csdsAuditWriteService, csdsAuditFailurePersistenceService);
        ReflectionTestUtils.setField(service, "schema", "appreg");
    }

    @Test
    void given_configuredAuditLevel_when_auditLevel_then_returnEnum() {
        when(jdbcTemplate.queryForObject(
                        anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn("DEBUG");

        assertThat(service.auditLevel()).isEqualTo(CsdsAuditLevel.DEBUG);
    }

    @Test
    void given_missingAuditLevel_when_auditLevel_then_throw() {
        when(jdbcTemplate.queryForObject(
                        anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(service::auditLevel)
                .isInstanceOf(AppRegistryException.class)
                .hasMessageContaining("AUDIT_CSDS configuration parameter is missing");
    }

    @Test
    void given_debugLevel_when_persistSuccessAudits_then_writeBatch() {
        var audit = new CsdsAuditEntry("APPLICATION_CODES", "INSERT", 1L, "{}", null);

        service.persistSuccessAudits(CsdsAuditLevel.DEBUG, List.of(audit));

        verify(csdsAuditWriteService).persist(List.of(audit));
    }

    @Test
    void given_errorLevel_when_persistSuccessAudits_then_skipWrite() {
        service.persistSuccessAudits(
                CsdsAuditLevel.ERROR,
                List.of(new CsdsAuditEntry("APPLICATION_CODES", "INSERT", 1L, "{}", null)));

        verify(csdsAuditWriteService, never()).persist(any());
    }

    @Test
    void given_errorLevel_when_persistFailureAudits_then_writeInNewTransaction() {
        var audit = new CsdsAuditEntry("APPLICATION_CODES", "INSERT", 1L, "{}", "boom");

        service.persistFailureAudits(CsdsAuditLevel.ERROR, List.of(audit));

        verify(csdsAuditFailurePersistenceService).persist(List.of(audit));
    }
}
