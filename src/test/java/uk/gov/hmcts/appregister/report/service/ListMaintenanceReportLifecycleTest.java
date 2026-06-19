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
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;

class ListMaintenanceReportLifecycleTest {
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

        ListMaintenanceReportLifecycle lifecycle = new ListMaintenanceReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
            lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            assertThat(csv.get()).contains("List Maintenance Report");
            Assertions.assertTrue(
                    csv.get().contains("List Date,List Court House Name,List Other Location"));
            assertThat(csv.get()).contains("18/05/2018,B01IX00 - Westminster");
            assertThat(csv.get()).contains("Other court,01,Morning list,OPEN,69");
            assertThat(csv.get()).contains(",,,,,,");
        } finally {
            outputFile.delete();
        }
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        ListMaintenanceReportLifecycle lifecycle = new ListMaintenanceReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            Assertions.assertTrue(outputFile.exists());

            lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

            Assertions.assertFalse(outputFile.exists());
        } finally {
            outputFile.delete();
        }
    }

    private AsyncJobLifecycleEvent<ListMaintenanceReportRow> event(
            JobStatusResponse response, List<ListMaintenanceReportRow> data, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private ListMaintenanceReportRow populatedRow() {
        return ListMaintenanceReportRow.builder()
                .listDate(LocalDate.of(2018, Month.MAY, 18))
                .courthouseName("B01IX00 - Westminster")
                .otherCourthouse("Other court")
                .cjaCode("01")
                .listDescription("Morning list")
                .listStatus("OPEN")
                .applicationEntryCount(69L)
                .build();
    }

    private ListMaintenanceReportRow blankRow() {
        return ListMaintenanceReportRow.builder().build();
    }

    private File getOutputFile(ListMaintenanceReportLifecycle lifecycle) {
        return lifecycle.outputFile();
    }
}
