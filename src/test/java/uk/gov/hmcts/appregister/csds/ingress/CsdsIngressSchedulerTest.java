package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsdsIngressSchedulerTest {
    @Mock private CsdsIngressProcessor csdsIngressProcessor;
    @InjectMocks private CsdsIngressScheduler scheduler;

    @Test
    void given_jobRuns_when_runNightlyIngress_then_logsCompletion() {
        var logCaptor = LogCaptor.forClass(CsdsIngressScheduler.class);
        logCaptor.clearLogs();
        when(csdsIngressProcessor.runIngress()).thenReturn(true);

        scheduler.runNightlyIngress();

        verify(csdsIngressProcessor).runIngress();
        assertThat(logCaptor.getInfoLogs())
                .contains("Running scheduled CSDS ingress", "Completed scheduled CSDS ingress")
                .doesNotContain(
                        "Skipping scheduled CSDS ingress because the job is disabled or the distributed lease is not"
                                + " available");
    }

    @Test
    void given_jobDoesNotRun_when_runNightlyIngress_then_logsSkip() {
        var logCaptor = LogCaptor.forClass(CsdsIngressScheduler.class);
        logCaptor.clearLogs();
        when(csdsIngressProcessor.runIngress()).thenReturn(false);

        scheduler.runNightlyIngress();

        verify(csdsIngressProcessor).runIngress();
        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "Running scheduled CSDS ingress",
                        "Skipping scheduled CSDS ingress because the job is disabled or the distributed lease is not"
                                + " available")
                .doesNotContain("Completed scheduled CSDS ingress");
    }
}
