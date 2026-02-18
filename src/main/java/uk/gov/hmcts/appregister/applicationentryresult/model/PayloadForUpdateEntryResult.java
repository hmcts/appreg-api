package uk.gov.hmcts.appregister.applicationentryresult.model;

import java.util.UUID;
import lombok.Getter;
import uk.gov.hmcts.appregister.common.model.PayloadForUpdate;
import uk.gov.hmcts.appregister.generated.model.ResultUpdateDto;

/**
 * A payload that represents the list id, entry id, and result id for an update to take place.
 */
@Getter
public class PayloadForUpdateEntryResult extends PayloadForUpdate<ResultUpdateDto> {
    private final UUID entryId;
    private final UUID resultId;

    public PayloadForUpdateEntryResult(
            ResultUpdateDto data, UUID listId, UUID entryId, UUID resultId) {
        super(data, listId);
        this.entryId = entryId;
        this.resultId = resultId;
    }
}
