package uk.gov.hmcts.appregister.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.admin.mapper.DatabaseJobsMapper;
import uk.gov.hmcts.appregister.common.entity.repository.DatabaseJobRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
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
        return databaseJobsMapper.toDatabaseJobStatus(
                databaseJobRepository.findByName(jobName.getValue()));
    }

    @Override
    public void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable) {
        var databaseJob = databaseJobRepository.findByName(jobName.getValue());
        databaseJob.setEnabled(enable ? YesOrNo.YES : YesOrNo.NO);
        databaseJobRepository.save(databaseJob);
    }
}
