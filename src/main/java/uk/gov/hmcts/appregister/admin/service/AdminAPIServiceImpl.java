package uk.gov.hmcts.appregister.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.admin.exception.JobError;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAPIServiceImpl implements AdminAPIService {
    private final DatabaseJobRepository databaseJobRepository;
    private final DatabaseJobsMapper databaseJobsMapper;

    @Override
    public JobStatus getDatabaseJobStatusByName(AdminJobType jobName) {
        var databaseJob = databaseJobRepository.findByName(jobName.getValue());
        if (databaseJob == null) {
            log.error("Database job with name {} not found", jobName.getValue());
            throw new AppRegistryException(JobError.JOB_NOT_FOUND, "Database Job not found");
        }
        return databaseJobsMapper.toDatabaseJobStatus(
                databaseJobRepository.findByName(jobName.getValue()));
    }

    @Override
    public void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable) {
        var databaseJob = databaseJobRepository.findByName(jobName.getValue());
        if (databaseJob == null) {
            log.error("Database job with name {} not found", jobName);
            throw new AppRegistryException(JobError.JOB_NOT_FOUND, "Database Job not found");
        }
        databaseJob.setEnabled(enable ? YesOrNo.YES : YesOrNo.NO);
        databaseJobRepository.save(databaseJob);
    }
}
