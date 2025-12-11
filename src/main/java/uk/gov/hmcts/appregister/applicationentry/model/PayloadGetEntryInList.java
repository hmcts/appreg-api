package uk.gov.hmcts.appregister.applicationentry.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PayloadGetEntryInList {
    private final UUID entryId;
    private final UUID listId;
}
