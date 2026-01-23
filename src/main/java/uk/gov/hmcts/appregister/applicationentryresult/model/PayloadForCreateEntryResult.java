package uk.gov.hmcts.appregister.applicationentryresult.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payload that represents the parent (listId), the target (entryId) and the incoming payload.
 */
@RequiredArgsConstructor
@Getter
@Builder
public class PayloadForCreateEntryResult<T> {
    private final UUID listId;
    private final UUID entryId;
    private final T data;
}
