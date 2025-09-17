package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.List;

/** Service interface for application actions. */
public interface ApplicationActionsService {
    void moveApplications(List<Long> applicationIds, Long targetListId);
}
