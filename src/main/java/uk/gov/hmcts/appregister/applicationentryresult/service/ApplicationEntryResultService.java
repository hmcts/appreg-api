package uk.gov.hmcts.appregister.applicationentryresult.service;

import java.util.UUID;

public interface ApplicationEntryResultService {

    /**
     * Deletes an Application List Entry Result.
     *
     * @param listId Public identifier of the Application List. (required)
     * @param entryId Public identifier of the Application List Entry. (required)
     * @param resultId Public identifier of the Application List Entry Result. (required)
     */
    void delete(UUID listId, UUID entryId, UUID resultId);
}
