package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

@Getter
@Setter
public class PartialFilterData<T extends Keyable> extends FilterFieldData<T> {
    /** The partial value. */
    private String startsWith;

    private String middleWith;

    private String endsWith;

    private String matchOnAllPartials;

    public PartialFilterData() {
    }

    @Override
    public PartialFilterData<T> deepClone() {
        return new PartialFilterData<>(this);
    }

    public PartialFilterData(PartialFilterData<T> filterFieldData) {
        super(filterFieldData);
        this.startsWith = filterFieldData.startsWith;
        this.middleWith = filterFieldData.middleWith;
        this.endsWith = filterFieldData.endsWith;
        this.matchOnAllPartials = filterFieldData.matchOnAllPartials;
    }
}
