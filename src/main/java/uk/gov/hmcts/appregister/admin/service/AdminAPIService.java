package uk.gov.hmcts.appregister.admin.service;

import uk.gov.hmcts.appregister.generated.model.AdminJobStatus;
import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobRetentionPolicy;

public interface AdminAPIService {
    AdminJobStatus getDatabaseJobStatusByName(AdminJobType jobName);

    void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable);

    JobRetentionPolicy getDatabaseJobRetentionPeriodByName(AdminJobType jobName);

    void updateDatabaseJobRetentionPeriodByName(AdminJobType jobName, Integer retentionPeriodDays);
}
