package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * A partial filter field data. This class sets the specific filter field values that we can use to
 * partially match on the underlying {@link FilterFieldValue} keyable.
 */
@Getter
@Setter
@NoArgsConstructor
public class PartialFilterFieldData<T extends Keyable> extends FilterFieldData<T> {
    /** The partial start with value. */
    private String startsWith;

    /** The middle partial match. */
    private String middleWith;

    /** The end with match value. */
    private String endsWith;

    /** The match on all partials match string. */
    private String matchOnAllPartials;

    @Override
    public PartialFilterFieldData<T> deepClone() {
        return new PartialFilterFieldData<>(this);
    }

    public PartialFilterFieldData(PartialFilterFieldData<T> filterFieldData) {
        super(filterFieldData);
        this.startsWith = filterFieldData.startsWith;
        this.middleWith = filterFieldData.middleWith;
        this.endsWith = filterFieldData.endsWith;
        this.matchOnAllPartials = filterFieldData.matchOnAllPartials;
    }
}
