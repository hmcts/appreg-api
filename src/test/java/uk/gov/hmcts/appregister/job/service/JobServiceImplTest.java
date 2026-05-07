package uk.gov.hmcts.appregister.job.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.service.AuditOperationServiceImpl;
import uk.gov.hmcts.appregister.common.async.model.JobStatusResponse;
import uk.gov.hmcts.appregister.common.entity.AsyncJob;
import uk.gov.hmcts.appregister.generated.model.JobAcknowledgement;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;
import uk.gov.hmcts.appregister.generated.model.JobType;
import uk.gov.hmcts.appregister.job.mapper.JobMapper;
import uk.gov.hmcts.appregister.job.validator.JobExistanceValidator;
import uk.gov.hmcts.appregister.job.validator.JobSuccess;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobMapper jobMapper;

    @Mock private JobExistanceValidator jobExistanceValidator;

    @Test
    void testGetJobAckById_auditsRequestedJobId() {
        val jobId = UUID.randomUUID();
        val responseDto = new JobAcknowledgement();
        responseDto.setId(jobId);
        responseDto.setStatus(JobStatus1.COMPLETED);
        responseDto.setType(JobType.FEES_REPORT);

        val jobStatusResponse =
                JobStatusResponse.builder()
                        .uuid(jobId)
                        .status(JobStatus1.COMPLETED)
                        .type(JobType.FEES_REPORT)
                        .userName("tenant:oid")
                        .build();
        val auditEntity = AsyncJob.builder().id(0L).uuid(jobId).build();

        when(jobExistanceValidator.validate(eq(jobId), any()))
                .thenAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            val validateFunction =
                                    (BiFunction<UUID, JobSuccess, JobStatusResponse>)
                                            invocation.getArgument(1);

                            val success = new JobSuccess();
                            success.setJobStatusResponse(jobStatusResponse);
                            return validateFunction.apply(jobId, success);
                        });
        when(jobMapper.toDto(jobStatusResponse)).thenReturn(responseDto);
        when(jobMapper.toEntity(jobId)).thenReturn(auditEntity);

        val listener = new CapturingAuditListener();
        val service =
                new JobServiceImpl(
                        jobMapper,
                        jobExistanceValidator,
                        new AuditOperationServiceImpl(new ObjectMapper(), List.of(listener)),
                        List.of(listener));

        // Execute the same service method used by the controller and capture the audit event that
        // is emitted when the request completes.
        val actual = service.getJobAckById(jobId);

        // The business response should still be the mapped acknowledgement DTO.
        Assertions.assertEquals(jobId, actual.getId());
        Assertions.assertEquals(JobStatus1.COMPLETED, actual.getStatus());
        Assertions.assertEquals(JobType.FEES_REPORT, actual.getType());

        // The auditable surrogate should contain the requested job UUID so the data-audit layer
        // can persist a GET row for asynch_jobs.id.
        Assertions.assertNotNull(listener.getCompleteEvent());
        val audited = (AsyncJob) listener.getCompleteEvent().getNewValue();
        Assertions.assertSame(auditEntity, audited);
        Assertions.assertEquals(jobId, audited.getUuid());
    }

    private static final class CapturingAuditListener implements AuditOperationLifecycleListener {
        private CompleteEvent completeEvent;

        @Override
        public void eventPerformed(BaseAuditEvent event) {
            if (event instanceof CompleteEvent complete) {
                completeEvent = complete;
            }
        }

        private CompleteEvent getCompleteEvent() {
            return completeEvent;
        }
    }
}
