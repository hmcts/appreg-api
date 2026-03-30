package uk.gov.hmcts.appregister.admin.service;

import uk.gov.hmcts.appregister.generated.model.DatabaseJobStatus;

public interface AdminAPIService {
    DatabaseJobStatus getDatabaseJobStatusByName(String jobName);

    void enableDisableDatabaseJobByName(String jobName, Boolean enable);
}
