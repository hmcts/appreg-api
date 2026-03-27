package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * Maps the keyable data to a generic object.
 */
@Setter
@Getter
public class FilterFieldData<T extends Keyable> {
    private FilterFieldDataDescriptor descriptor;
    private FilterValue<T> keyableValues;

    public FilterFieldData() {}

    public FilterFieldData<T> deepClone() {
        return new FilterFieldData<>(this);
    }

    public FilterFieldData(FilterFieldData<T> filterFieldData) {
        setDescriptor(filterFieldData.descriptor);
        setKeyableValues(new FilterValue<>(filterFieldData.keyableValues));
    }
}
