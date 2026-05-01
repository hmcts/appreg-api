package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.async.AbstractAsyncTest;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.report.job.DurationReportLifecycle;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportLifecycleTest extends AbstractAsyncTest {
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

        DurationReportLifecycle lifecycle = new DurationReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
            lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            Assertions.assertTrue(csv.get().contains("Duration Report"));
            Assertions.assertTrue(csv.get().contains("List Date,List Court House Name"));
            Assertions.assertTrue(csv.get().contains("18/05/2018,B01IX00 - Westminster"));
            Assertions.assertTrue(csv.get().contains("Other court,01,Morning list,2,45"));
            Assertions.assertTrue(csv.get().contains(",,,,,,"));
        } finally {
            Files.deleteIfExists(outputFile.toPath());
        }
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        DurationReportLifecycle lifecycle = new DurationReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            Assertions.assertTrue(outputFile.exists());

            lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

            Assertions.assertFalse(outputFile.exists());
        } finally {
            Files.deleteIfExists(outputFile.toPath());
        }
    }

    private AsyncJobLifecycleEvent<DurationReportRow> event(
            JobStatusResponse response, List<DurationReportRow> data, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private DurationReportRow populatedRow() {
        return DurationReportRow.builder()
                .listDate(LocalDate.of(2018, 5, 18))
                .courthouseName("B01IX00 - Westminster")
                .otherCourthouse("Other court")
                .cjaCode("01")
                .listDescription("Morning list")
                .durationHours(2)
                .durationMinutes(45)
                .build();
    }

    private DurationReportRow blankRow() {
        return DurationReportRow.builder().build();
    }

    private File getOutputFile(DurationReportLifecycle lifecycle) {
        return (File) ReflectionTestUtils.getField(lifecycle, "file");
    }
}
