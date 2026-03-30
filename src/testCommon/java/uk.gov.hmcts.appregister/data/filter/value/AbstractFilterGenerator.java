package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;
import uk.gov.hmcts.appregister.data.filter.FilterValue;

/**
 * A filter value generator that generates a value for a filter descriptor.
 */
public abstract class AbstractFilterGenerator<T extends Keyable> implements GenerateAccordingToFilter<T> {
    public AbstractFilterGenerator() {}

    @Override
    public FilterFieldData<T> apply(int count, T keyable, FilterFieldDataDescriptor<T> descriptor) {
        FilterFieldData<T> filterFieldData = FilterUtil.getFieldDataWithString(count, descriptor, keyable ,100);
        setValue(count, filterFieldData.getKeyableValues().getValue().toString(), filterFieldData.getKeyableValues(), descriptor);

        return filterFieldData;
    }

    protected abstract void setValue(int count, String obj,
                                        FilterValue<T> value,
                                        FilterFieldDataDescriptor<T> descriptor);


}
