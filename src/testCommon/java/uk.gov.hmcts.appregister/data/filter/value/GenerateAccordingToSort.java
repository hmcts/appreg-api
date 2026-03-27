package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.sort.SortDataDescriptor;

@FunctionalInterface
public interface GenerateAccordingToSort<T> {
    void apply(T keyable, SortDataDescriptor<T> descriptor, OrderEnum orderEnum);
}
