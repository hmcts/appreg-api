package uk.gov.hmcts.appregister.csds.ingress.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@ExtendWith(MockitoExtension.class)
class JdbcIngressBackupServiceTest {
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private CsdsIngressTransactionRunner csdsIngressTransactionRunner;

    @InjectMocks private JdbcIngressBackupService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "schema", "appreg");
    }

    @Test
    void given_sourceAndTargetTables_when_backup_then_deleteAndReloadTargetInTransaction() {
        stubTransactionRunner();
        when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(2, 3);

        var result = service.backup("application_codes", "application_codes_staging");

        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture(), anyMap());
        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(result.insertedCount()).isEqualTo(3);
        assertThat(sqlCaptor.getAllValues())
                .containsExactly(
                        "DELETE FROM appreg.application_codes_staging",
                        """
                        INSERT INTO appreg.application_codes_staging
                        SELECT * FROM appreg.application_codes
                        """);
    }

    @Test
    void given_invalidSourceTable_when_backup_then_rejectIt() {
        assertThatThrownBy(() -> service.backup("application-codes", "application_codes_staging"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL sourceTable");
    }

    private void stubTransactionRunner() {
        when(csdsIngressTransactionRunner.execute(
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
    }
}
