package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;

@FunctionalInterface
public interface GenerateAccordingToFilter<T extends Keyable> {
    FilterFieldData<T> apply(T keyable, FilterFieldDataDescriptor<T> descriptor, OrderEnum orderEnum);
}
