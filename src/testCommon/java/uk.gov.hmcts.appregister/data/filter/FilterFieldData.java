package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;

/**
 * A filter field data that maps the descriptor to a filter value.
 */
@Setter
@Getter
@NoArgsConstructor
public class FilterFieldData<T extends Keyable> {

    /** The filter meta data descriptor. */
    private FilterFieldDataMetaDescriptor<T> descriptor;

    /** The filter value that stores a keyable. */
    private FilterFieldValue<T> keyableValues;

    /**
     * Deep clones the filter field data.
     *
     * @return The deep clone filter field data.
     */
    public FilterFieldData<T> deepClone() {
        return new FilterFieldData<>(this);
    }

    public FilterFieldData(FilterFieldData<T> filterFieldData) {
        setDescriptor(filterFieldData.descriptor);
        setKeyableValues(new FilterFieldValue<>(filterFieldData.keyableValues));
    }
}
