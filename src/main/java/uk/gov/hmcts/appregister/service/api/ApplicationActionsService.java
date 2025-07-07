package uk.gov.hmcts.appregister.service.api;

import java.util.List;

public interface ApplicationActionsService {
    void moveApplications(List<Long> applicationIds, Long targetListId, String userId);
}
