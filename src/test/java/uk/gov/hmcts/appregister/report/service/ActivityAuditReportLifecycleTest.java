package uk.gov.hmcts.appregister.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.report.model.ActivityAuditReportRow;

class ActivityAuditReportLifecycleTest {
    @Test
    void givenReportRows_whenCompleted_thenWritesCsvAndDeletesTempFile() throws Exception {
        AtomicReference<String> csv = new AtomicReference<>();
        JobStatusResponse response = mock(JobStatusResponse.class);
        doAnswer(
                        invocation -> {
                            InputStream inputStream = invocation.getArgument(0);
                            csv.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                            return null;
                        })
                .when(response)
                .write(any(InputStream.class));

        ActivityAuditReportLifecycle lifecycle = new ActivityAuditReportLifecycle();
        final File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
            lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            assertThat(csv.get()).contains("Activity Audit Report");
            assertThat(csv.get()).contains("Event Name,Table Name,Column Name");
            Assertions.assertTrue(
                    csv.get().contains("Add Application,APPLICATION_LIST_ENTRY,APPLICATION_CODE"));
            assertThat(csv.get()).contains("old,new,2026-04-01,caseworker");
            assertThat(csv.get()).contains(",,,,,,");
        } finally {
            outputFile.delete();
        }
    }

    @Test
    void givenNoRows_whenCompleted_thenWritesHeadersAndDeletesTempFile() throws Exception {
        AtomicReference<String> csv = new AtomicReference<>();
        JobStatusResponse response = mock(JobStatusResponse.class);
        doAnswer(
                        invocation -> {
                            InputStream inputStream = invocation.getArgument(0);
                            csv.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                            return null;
                        })
                .when(response)
                .write(any(InputStream.class));

        ActivityAuditReportLifecycle lifecycle = new ActivityAuditReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            assertThat(csv.get()).contains("Activity Audit Report");
            assertThat(csv.get()).contains("Event Name,Table Name,Column Name");
        } finally {
            outputFile.delete();
        }
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        ActivityAuditReportLifecycle lifecycle = new ActivityAuditReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            Assertions.assertTrue(outputFile.exists());

            lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

            Assertions.assertFalse(outputFile.exists());
        } finally {
            outputFile.delete();
        }
    }

    private AsyncJobLifecycleEvent<ActivityAuditReportRow> event(
            JobStatusResponse response, List<ActivityAuditReportRow> data, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private ActivityAuditReportRow populatedRow() {
        return ActivityAuditReportRow.builder()
                .eventName("Add Application")
                .tableName("APPLICATION_LIST_ENTRY")
                .columnName("APPLICATION_CODE")
                .oldValue("old")
                .newValue("new")
                .createdDate(LocalDate.of(2026, Month.APRIL, 1))
                .userName("caseworker")
                .build();
    }

    private ActivityAuditReportRow blankRow() {
        return ActivityAuditReportRow.builder().build();
    }

    private File getOutputFile(ActivityAuditReportLifecycle lifecycle) {
        return lifecycle.outputFile();
    }
}
