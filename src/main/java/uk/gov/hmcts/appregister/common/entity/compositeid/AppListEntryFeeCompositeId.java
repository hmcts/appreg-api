package uk.gov.hmcts.appregister.common.entity.compositeid;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A composite primary key id containing both the app list entry id and fee id.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class AppListEntryFeeCompositeId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appListEntryId;

    private Long feeId;

    public AppListEntryFeeCompositeId(Long appListEntryId, Long feeId) {
        this.appListEntryId = appListEntryId;
        this.feeId = feeId;
    }
}
