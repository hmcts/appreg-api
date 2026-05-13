package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;

/**
 * A payload that represents both the list id as well as the entry id for an update to take place.
 */
@Getter
public class PayloadForDeleteEntry extends PayloadForUpdate<Void> {
    private final UUID entryId;

    public PayloadForDeleteEntry(UUID listId, UUID entryId) {
        super(null, listId);
        this.entryId = entryId;
    }
}
