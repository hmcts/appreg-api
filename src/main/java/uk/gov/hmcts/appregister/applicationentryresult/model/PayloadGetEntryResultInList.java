package uk.gov.hmcts.appregister.applicationentryresult.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayloadGetEntryResultInList {
    private final UUID entryId;
    private final UUID listId;
}
