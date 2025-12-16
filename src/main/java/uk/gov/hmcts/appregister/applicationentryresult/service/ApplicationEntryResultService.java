package uk.gov.hmcts.appregister.applicationentryresult.service;

import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;

/**
 * Service interface for managing application list entry results.
 */
public interface ApplicationEntryResultService {
    void delete(ListEntryResultDeleteArgs args);
}
