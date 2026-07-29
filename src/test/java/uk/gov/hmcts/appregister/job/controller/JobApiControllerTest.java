package uk.gov.hmcts.appregister.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.service.JobService;

class JobApiControllerTest {
    private final JobService jobService = mock(JobService.class);
    private final JobApiController controller = new JobApiController(jobService);

    @Test
    void getJobStatusById_delegatesAndReturnsVersionedOk() {
        UUID jobId = UUID.randomUUID();
        var body =
                new JobAcknowledgement()
                        .id(jobId)
                        .type(JobType.FEES_REPORT)
                        .status(JobStatus.RECEIVED);
        when(jobService.getJobAckById(jobId)).thenReturn(body);

        ResponseEntity<JobAcknowledgement> actual = controller.getJobStatusById(jobId);

        verify(jobService).getJobAckById(jobId);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getBody()).isSameAs(body);
        assertThat(actual.getHeaders().getVary()).containsExactly("Accept");
        assertThat(actual.getHeaders().getContentType())
                .hasToString("application/vnd.hmcts.appreg.v1+json");
    }
}
