package uk.gov.hmcts.appregister.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.lifecycle.AsyncJobLifecycleEvent;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.util.AppRegTempFileUtil;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

class FeesReportLifecycleTest {
    private static final String TEMP_FILE_PREFIX = "fees-report-";

    @AfterEach
    void tearDown() {
        for (File file : getFeesReportTempFiles()) {
            file.delete();
        }
    }

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

        FeesReportLifecycle lifecycle = new FeesReportLifecycle();
        final File outputFile = getOutputFile(lifecycle);
        lifecycle.processing(event(response, List.of(populatedRow()), JobStatus1.PROCESSING));
        lifecycle.processing(event(response, List.of(blankRow()), JobStatus1.PROCESSING));
        lifecycle.completed(event(response, List.of(), JobStatus1.COMPLETED));

        Assertions.assertFalse(outputFile.exists());
        Assertions.assertTrue(csv.get().contains("Fees Report"));
        Assertions.assertTrue(csv.get().contains("List Date,List Court House Name"));
        Assertions.assertTrue(csv.get().contains("18/05/2018,B01IX00 - Westminster"));
        Assertions.assertTrue(csv.get().contains("20.13,1.00,21.13,Due,2018-12-03,REF-1"));
        Assertions.assertTrue(csv.get().contains(",,,,,,,,,,,,,"));
    }

    @Test
    void givenLifecycleIsFailed_thenDeletesTempFile() throws Exception {
        FeesReportLifecycle lifecycle = new FeesReportLifecycle();
        File outputFile = getOutputFile(lifecycle);
        Assertions.assertTrue(outputFile.exists());

        lifecycle.failed(event(mock(JobStatusResponse.class), List.of(), JobStatus1.FAILED));

        Assertions.assertFalse(outputFile.exists());
    }

    private AsyncJobLifecycleEvent<FeesReportRow> event(
            JobStatusResponse response, List<FeesReportRow> data, JobStatus1 status) {
        return new AsyncJobLifecycleEvent<>(response, data, mock(JobContext.class), status);
    }

    private FeesReportRow populatedRow() {
        return FeesReportRow.builder()
                .listDate(LocalDate.of(2018, 5, 18))
                .courthouseName("B01IX00 - Westminster")
                .otherCourthouse("Other court")
                .cjaCode("01")
                .standardApplicantCode("STD1")
                .applicantFullName("British Gas")
                .applicationCode("RE99001")
                .applicationCodeTitle("Rights of Entry Warrant")
                .feeValue(BigDecimal.valueOf(20.125))
                .offSiteFeeValue(BigDecimal.ONE)
                .totalFeeValue(BigDecimal.valueOf(21.125))
                .feeStatus("Due")
                .feeStatusDate(LocalDate.of(2018, 12, 3))
                .paymentReference("REF-1")
                .build();
    }

    private FeesReportRow blankRow() {
        return FeesReportRow.builder().build();
    }

    private File getOutputFile(FeesReportLifecycle lifecycle) {
        return (File) ReflectionTestUtils.getField(lifecycle, "file");
    }

    private File[] getFeesReportTempFiles() {
        File[] files =
                new File(System.getProperty("java.io.tmpdir"))
                        .listFiles(
                                file ->
                                        file.getName().startsWith(TEMP_FILE_PREFIX)
                                                && file.getName()
                                                        .endsWith(
                                                                "."
                                                                        + AppRegTempFileUtil
                                                                                .TEMP_FILE_EXTENSION));
        return files == null ? new File[0] : files;
    }
}
