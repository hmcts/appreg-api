package uk.gov.hmcts.appregister.applicationentryresult.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Payload that represents the parent (listId), whos entries will be resulted.
 */
@RequiredArgsConstructor
@Getter
@Builder
public class PayloadForCreateResults<T> {
    /**
     * Make the list optional as we have a requirement to reuse this functionality where a list id
     * may not exist.
     */
    private final UUID listId;

    private final T payload;
}
