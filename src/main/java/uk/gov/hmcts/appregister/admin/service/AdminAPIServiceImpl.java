package uk.gov.hmcts.appregister.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.admin.exception.DatabaseJobError;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAPIServiceImpl implements AdminAPIService {
    private final DatabaseJobRepository databaseJobRepository;
    private final DatabaseJobsMapper databaseJobsMapper;

    @Override
    public DatabaseJobStatus getDatabaseJobStatusByName(String jobName) {
        var databaseJob = databaseJobRepository.findByName(jobName);
        if (databaseJob == null) {
            log.error("Database job with name {} not found", jobName);
            throw new AppRegistryException(
                    DatabaseJobError.JOB_NOT_FOUND, "Database Job not found");
        }
        return databaseJobsMapper.toDatabaseJobStatus(databaseJobRepository.findByName(jobName));
    }

    @Override
    public void enableDisableDatabaseJobByName(String jobName, Boolean enable) {
        var databaseJob = databaseJobRepository.findByName(jobName);
        if (databaseJob == null) {
            log.error("Database job with name {} not found", jobName);
            throw new AppRegistryException(
                    DatabaseJobError.JOB_NOT_FOUND, "Database Job not found");
        }
        databaseJob.setEnabled(enable ? YesOrNo.YES : YesOrNo.NO);
        databaseJobRepository.save(databaseJob);
    }
}
