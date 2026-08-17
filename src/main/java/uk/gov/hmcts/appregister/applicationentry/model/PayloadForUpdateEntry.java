package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateDto;

/**
 * A payload that represents both the list id as well as the entry id for an update to take place.
 */
@Getter
public class PayloadForUpdateEntry extends PayloadForUpdate<EntryUpdateDto> {
    private final UUID entryId;
    private final ApplicationListEntry applicationListEntry;

    public PayloadForUpdateEntry(EntryUpdateDto data, UUID listId, UUID entryId) {
        this(data, listId, entryId, null);
    }

    private PayloadForUpdateEntry(
            EntryUpdateDto data,
            UUID listId,
            UUID entryId,
            ApplicationListEntry applicationListEntry) {
        super(data, listId);
        this.entryId = entryId;
        this.applicationListEntry = applicationListEntry;
    }

    /** Returns this request enriched with the entry already resolved during path validation. */
    public PayloadForUpdateEntry withApplicationListEntry(
            ApplicationListEntry applicationListEntry) {
        return new PayloadForUpdateEntry(getData(), getId(), entryId, applicationListEntry);
    }
}
