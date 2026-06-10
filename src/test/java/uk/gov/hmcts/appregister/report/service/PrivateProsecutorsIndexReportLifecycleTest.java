package uk.gov.hmcts.appregister.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;

class PrivateProsecutorsIndexReportLifecycleTest {
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

        PrivateProsecutorsIndexReportLifecycle lifecycle =
                new PrivateProsecutorsIndexReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
            lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
            lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

            Assertions.assertFalse(outputFile.exists());
            assertThat(csv.get()).contains("Private Prosecution Index Report");
            assertThat(csv.get()).contains("List Date,List Court House Name");
            assertThat(csv.get()).contains("18/05/2018,B01IX00 - Westminster");
            assertThat(csv.get()).contains("Smith,John,,Jane,Bloggs,Widgets Ltd");
            assertThat(csv.get()).contains("Wording,R4,R3,R2,R1,Notes");
            assertThat(csv.get()).contains(",,,,,,,,,,,,,,,");
        } finally {
            outputFile.delete();
        }
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        PrivateProsecutorsIndexReportLifecycle lifecycle =
                new PrivateProsecutorsIndexReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        try {
            Assertions.assertTrue(outputFile.exists());

            lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

            Assertions.assertFalse(outputFile.exists());
        } finally {
            outputFile.delete();
        }
    }

    private AsyncJobLifecycleEvent<PrivateProsecutorsIndexReportRow> event(
            JobStatusResponse response,
            List<PrivateProsecutorsIndexReportRow> data,
            JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private PrivateProsecutorsIndexReportRow populatedRow() {
        return PrivateProsecutorsIndexReportRow.builder()
                .listDate(LocalDate.of(2018, 5, 18))
                .courthouseName("B01IX00 - Westminster")
                .otherCourthouse("Other court")
                .cjaCode("01")
                .applicantNameOrSurname("Smith")
                .applicantFirstName("John")
                .respondentFirstName("Jane")
                .respondentSurname("Bloggs")
                .respondentOrganisationName("Widgets Ltd")
                .applicationWording("Wording")
                .result1("R4")
                .result2("R3")
                .result3("R2")
                .result4("R1")
                .notes("Notes")
                .build();
    }

    private PrivateProsecutorsIndexReportRow blankRow() {
        return PrivateProsecutorsIndexReportRow.builder().build();
    }

    private File getOutputFile(PrivateProsecutorsIndexReportLifecycle lifecycle) {
        return (File) ReflectionTestUtils.getField(lifecycle, "file");
    }
}
