package uk.gov.hmcts.appregister.applicationentry.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayloadGetEntryInList {
    private final UUID entryId;
    private final UUID listId;
}
