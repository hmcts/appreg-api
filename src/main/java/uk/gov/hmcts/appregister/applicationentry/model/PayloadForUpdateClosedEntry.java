package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;

/**
 * A payload that represents both the list id as well as the entry id for an update to take place.
 */
@Getter
public class PayloadForUpdateClosedEntry extends PayloadForUpdate<EntryUpdateClosedDto> {
    private final UUID entryId;

    public PayloadForUpdateClosedEntry(EntryUpdateClosedDto data, UUID listId, UUID entryId) {
        super(data, listId);
        this.entryId = entryId;
    }
}
