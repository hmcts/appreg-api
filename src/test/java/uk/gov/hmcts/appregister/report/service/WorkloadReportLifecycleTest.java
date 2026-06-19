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
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

class WorkloadReportLifecycleTest {
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

        WorkloadReportLifecycle lifecycle = new WorkloadReportLifecycle();
        final File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
            lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            assertThat(csv.get()).contains("Workload Report");
            Assertions.assertTrue(
                    csv.get()
                            .contains(
                                    "List Date,List Court House Name,List Other Location,CJA Code,"
                                            + "List Description,Standard Applicant Code,"
                                            + "Applicant Name/Surname,Application Code,"
                                            + "Application Code Title,Results,JP1,JP2,JP3,Official"));
            Assertions.assertTrue(
                    csv.get().contains("18/05/2018,B01IX00 - Westminster,Other court,01,,STD1"));
            Assertions.assertTrue(
                    csv.get()
                            .contains(
                                    "British Gas,RE99001,Rights of Entry Warrant,"
                                            + "\"A,B,C\",JP1,,,Test Official"));
        } finally {
            outputFile.delete();
        }
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        WorkloadReportLifecycle lifecycle = new WorkloadReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            Assertions.assertTrue(outputFile.exists());

            lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

            Assertions.assertFalse(outputFile.exists());
        } finally {
            outputFile.delete();
        }
    }

    private AsyncJobLifecycleEvent<WorkloadReportRow> event(
            JobStatusResponse response, List<WorkloadReportRow> data, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private WorkloadReportRow populatedRow() {
        return WorkloadReportRow.builder()
                .listDate(LocalDate.of(2018, Month.MAY, 18))
                .listCourtHouseName("B01IX00 - Westminster")
                .listOtherLocation("Other court")
                .cjaCode("01")
                .standardApplicantCode("STD1")
                .applicantNameSurname("British Gas")
                .applicationCode("RE99001")
                .applicationCodeTitle("Rights of Entry Warrant")
                .jp1("JP1")
                .results("A,B,C")
                .official("Test Official")
                .build();
    }

    private WorkloadReportRow blankRow() {
        return WorkloadReportRow.builder().build();
    }

    private File getOutputFile(WorkloadReportLifecycle lifecycle) {
        return lifecycle.outputFile();
    }
}
