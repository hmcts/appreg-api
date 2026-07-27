package uk.gov.hmcts.appregister.csds.ingress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsdsIngressSchedulerTest {
    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");

    @Mock private CsdsIngressProperties properties;
    @Mock private CsdsIngressProcessor csdsIngressProcessor;

    @Test
    void given_jobRuns_when_pollNightlyIngress_then_logsCompletion() {
        var logCaptor = LogCaptor.forClass(CsdsIngressScheduler.class);
        logCaptor.clearLogs();
        var schedule = new CsdsIngressProperties.Schedule();
        schedule.setHour(3);
        schedule.setMinute(0);
        when(properties.getSchedule()).thenReturn(schedule);
        when(csdsIngressProcessor.hasTerminalStatusToday()).thenReturn(false);
        when(csdsIngressProcessor.runScheduledIngress(any()))
                .thenReturn(CsdsIngressProcessor.ScheduledRunResult.succeeded());
        var scheduler = schedulerAt("2026-07-27T03:05:00Z");

        scheduler.pollNightlyIngress();

        verify(csdsIngressProcessor).runScheduledIngress(any());
        assertThat(logCaptor.getInfoLogs())
                .contains("Running scheduled CSDS ingress", "Completed scheduled CSDS ingress");
    }

    @Test
    void given_terminalStatusExistsToday_when_pollNightlyIngress_then_doesNothing() {
        var schedule = new CsdsIngressProperties.Schedule();
        schedule.setHour(3);
        schedule.setMinute(0);
        var scheduler = schedulerAt("2026-07-27T03:05:00Z");
        when(properties.getSchedule()).thenReturn(schedule);
        when(csdsIngressProcessor.hasTerminalStatusToday()).thenReturn(true);

        scheduler.pollNightlyIngress();

        verify(csdsIngressProcessor, never()).runScheduledIngress(any());
    }

    @Test
    void given_beforeScheduledTime_when_pollNightlyIngress_then_doesNothing() {
        var schedule = new CsdsIngressProperties.Schedule();
        schedule.setHour(3);
        schedule.setMinute(0);
        var scheduler = schedulerAt("2026-07-27T01:59:00Z");
        when(properties.getSchedule()).thenReturn(schedule);

        scheduler.pollNightlyIngress();

        verify(csdsIngressProcessor, never()).hasTerminalStatusToday();
        verify(csdsIngressProcessor, never()).runScheduledIngress(any());
    }

    @Test
    void given_jobDoesNotAcquireLease_when_pollNightlyIngress_then_logsSkip() {
        var logCaptor = LogCaptor.forClass(CsdsIngressScheduler.class);
        logCaptor.clearLogs();
        var schedule = new CsdsIngressProperties.Schedule();
        schedule.setHour(3);
        schedule.setMinute(0);
        when(properties.getSchedule()).thenReturn(schedule);
        when(csdsIngressProcessor.hasTerminalStatusToday()).thenReturn(false);
        when(csdsIngressProcessor.runScheduledIngress(any()))
                .thenReturn(CsdsIngressProcessor.ScheduledRunResult.skippedLockUnavailable());
        var scheduler = schedulerAt("2026-07-27T03:05:00Z");

        scheduler.pollNightlyIngress();

        verify(csdsIngressProcessor).runScheduledIngress(any());
        assertThat(logCaptor.getInfoLogs())
                .contains(
                        "Skipping scheduled CSDS ingress because the job is disabled or the distributed lease is not"
                                + " available");
    }

    @Test
    void given_jobFails_when_pollNightlyIngress_then_recordsFailure() {
        var logCaptor = LogCaptor.forClass(CsdsIngressScheduler.class);
        logCaptor.clearLogs();
        var schedule = new CsdsIngressProperties.Schedule();
        schedule.setHour(3);
        schedule.setMinute(0);
        when(properties.getSchedule()).thenReturn(schedule);
        when(csdsIngressProcessor.hasTerminalStatusToday()).thenReturn(false);
        when(csdsIngressProcessor.runScheduledIngress(any()))
                .thenReturn(
                        CsdsIngressProcessor.ScheduledRunResult.failed("Failed processors: fee"));
        var scheduler = schedulerAt("2026-07-27T03:05:00Z");

        scheduler.pollNightlyIngress();

        assertThat(logCaptor.getInfoLogs()).contains("Running scheduled CSDS ingress");
        assertThat(logCaptor.getErrorLogs())
                .contains("Scheduled CSDS ingress failed: Failed processors: fee");
    }

    private CsdsIngressScheduler schedulerAt(String instant) {
        return new CsdsIngressScheduler(
                properties,
                csdsIngressProcessor,
                Clock.fixed(Instant.parse(instant), ZoneId.of("UTC")),
                UK_ZONE);
    }
}
