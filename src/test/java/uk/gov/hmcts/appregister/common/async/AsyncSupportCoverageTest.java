package uk.gov.hmcts.appregister.common.async;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.appregister.common.async.exception.JobException;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.async.service.AsyncJobPersistenceService;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;

class AsyncSupportCoverageTest {

    @Test
    void inTransaction_runsRunnableAndSupplier() {
        var unitOfWork = new TransactionUnitOfWork();
        var counter = new int[] {0};

        unitOfWork.inTransaction(() -> counter[0]++);
        var result = unitOfWork.inTransaction(() -> "done");

        assertEquals(1, counter[0]);
        assertEquals("done", result);
    }

    @Test
    void inTransaction_wrapsSupplierExceptions() {
        var unitOfWork = new TransactionUnitOfWork();

        var exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                unitOfWork.inTransaction(
                                        () -> {
                                            throw new IllegalStateException("broken");
                                        }));

        assertEquals("broken", exception.getCause().getMessage());
    }

    @Test
    void jobStatusResponse_allowsReadAndWriteForActiveJobs() throws IOException {
        var persistence = mock(AsyncJobPersistenceService.class);
        var uuid = UUID.randomUUID();
        var response =
                JobStatusResponse.builder()
                        .uuid(uuid)
                        .type(JobType.FEES_REPORT)
                        .status(JobStatus1.PROCESSING)
                        .userName("tester")
                        .errorMessage(null)
                        .persistence(persistence)
                        .build();
        var input = new ByteArrayInputStream("csv".getBytes(StandardCharsets.UTF_8));
        var resource =
                new InputStreamResource(
                        new ByteArrayInputStream("csv".getBytes(StandardCharsets.UTF_8)));
        when(persistence.readClob(response.getJobId())).thenReturn(resource);

        response.write(input);
        var read = response.read();

        verify(persistence).writeClob(response.getJobId(), input);
        verify(persistence).readClob(response.getJobId());
        assertSame(resource, read);
        assertEquals(uuid, response.getJobId().getId());
        assertEquals("tester", response.getJobId().getUserName());
    }

    @Test
    void jobStatusResponse_rejectsWritesForFinishedJobs() {
        var persistence = mock(AsyncJobPersistenceService.class);
        var input = new ByteArrayInputStream(new byte[0]);

        var completedResponse =
                JobStatusResponse.builder()
                        .uuid(UUID.randomUUID())
                        .type(JobType.FEES_REPORT)
                        .status(JobStatus1.COMPLETED)
                        .userName("tester")
                        .errorMessage(null)
                        .persistence(persistence)
                        .build();

        assertThrows(JobException.class, () -> completedResponse.write(input));
    }

    @Test
    void jobStatusResponse_allowsWriteForFailedJobs() {
        var persistence = mock(AsyncJobPersistenceService.class);
        var input = new ByteArrayInputStream(new byte[0]);

        var failedResponse =
                JobStatusResponse.builder()
                        .uuid(UUID.randomUUID())
                        .type(JobType.FEES_REPORT)
                        .status(JobStatus1.FAILED)
                        .userName("tester")
                        .errorMessage("bad")
                        .persistence(persistence)
                        .build();

        assertDoesNotThrow(() -> failedResponse.write(input));
    }
}
