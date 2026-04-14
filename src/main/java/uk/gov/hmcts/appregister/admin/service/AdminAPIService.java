package uk.gov.hmcts.appregister.admin.service;

import uk.gov.hmcts.appregister.generated.model.AdminJobType;
import uk.gov.hmcts.appregister.generated.model.JobStatus;

public interface AdminAPIService {
    JobStatus getDatabaseJobStatusByName(AdminJobType jobName);

    void enableDisableDatabaseJobByName(AdminJobType jobName, Boolean enable);
}
