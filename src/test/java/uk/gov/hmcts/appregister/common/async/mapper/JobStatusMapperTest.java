package uk.gov.hmcts.appregister.common.async.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.JobStatusType;
import uk.gov.hmcts.appregister.generated.model.JobStatus1;

public class JobStatusMapperTest {
    @Test
    public void testMap() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();
        Assertions.assertEquals(JobStatusType.SUBMITTED, mapper.getJobStatus(JobStatus1.RECEIVED));
        Assertions.assertEquals(JobStatusType.PENDING, mapper.getJobStatus(JobStatus1.VALIDATING));
        Assertions.assertEquals(JobStatusType.FAILED, mapper.getJobStatus(JobStatus1.FAILED));
        Assertions.assertEquals(JobStatusType.RUNNING, mapper.getJobStatus(JobStatus1.PROCESSING));
        Assertions.assertEquals(JobStatusType.COMPLETED, mapper.getJobStatus(JobStatus1.COMPLETED));
    }

    @Test
    public void testInverseMap() {
        JobStatusMapperImpl mapper = new JobStatusMapperImpl();

        Assertions.assertEquals(JobStatus1.RECEIVED, mapper.getJobStatus(JobStatusType.SUBMITTED));
        Assertions.assertEquals(JobStatus1.VALIDATING, mapper.getJobStatus(JobStatusType.PENDING));
        Assertions.assertEquals(JobStatus1.FAILED, mapper.getJobStatus(JobStatusType.FAILED));
        Assertions.assertEquals(JobStatus1.PROCESSING, mapper.getJobStatus(JobStatusType.RUNNING));
        Assertions.assertEquals(JobStatus1.COMPLETED, mapper.getJobStatus(JobStatusType.COMPLETED));
    }
}
