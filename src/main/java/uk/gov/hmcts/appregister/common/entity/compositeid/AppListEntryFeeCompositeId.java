package uk.gov.hmcts.appregister.common.entity.compositeid;

import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A composite primary key id containing both the app list entry id and fee id.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class AppListEntryFeeCompositeId implements Serializable {

    private Long appListEntryId;

    private Long feeId;

    /**
     * Create composite id.
     *
     * @param appListEntryId entryId
     * @param feeId feeId
     */
    public AppListEntryFeeCompositeId(Long appListEntryId, Long feeId) {
        this.appListEntryId = appListEntryId;
        this.feeId = feeId;
    }
}
