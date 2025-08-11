package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;

public interface ApplicationActionsService {
    void moveApplications(List<Long> applicationIds, Long targetListId, String userId);
}
