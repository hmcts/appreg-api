package uk.gov.hmcts.appregister.common.async.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

class JobStatusMapperTest {
    @Test
    void given_api_status_when_mapped_then_database_status_is_returned() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();
        Assertions.assertEquals(JobStatusType.SUBMITTED, mapper.getJobStatus(JobStatus.RECEIVED));
        Assertions.assertEquals(JobStatusType.PENDING, mapper.getJobStatus(JobStatus.VALIDATING));
        Assertions.assertEquals(JobStatusType.FAILED, mapper.getJobStatus(JobStatus.FAILED));
        Assertions.assertEquals(JobStatusType.RUNNING, mapper.getJobStatus(JobStatus.PROCESSING));
        Assertions.assertEquals(JobStatusType.COMPLETED, mapper.getJobStatus(JobStatus.COMPLETED));
    }

    @Test
    void given_database_status_when_mapped_then_api_status_is_returned() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();

        Assertions.assertEquals(JobStatus.RECEIVED, mapper.getJobStatus(JobStatusType.SUBMITTED));
        Assertions.assertEquals(JobStatus.VALIDATING, mapper.getJobStatus(JobStatusType.PENDING));
        Assertions.assertEquals(JobStatus.FAILED, mapper.getJobStatus(JobStatusType.FAILED));
        Assertions.assertEquals(JobStatus.PROCESSING, mapper.getJobStatus(JobStatusType.RUNNING));
        Assertions.assertEquals(JobStatus.COMPLETED, mapper.getJobStatus(JobStatusType.COMPLETED));
    }

    @Test
    void given_null_api_status_when_mapped_then_null_is_returned() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();

        Assertions.assertNull(mapper.getJobStatus((JobStatus) null));
    }

    @Test
    void given_null_database_status_when_mapped_then_null_is_returned() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();

        Assertions.assertNull(mapper.getJobStatus((JobStatusType) null));
    }
}
