package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;

@FunctionalInterface
public interface GenerateAccordingToFilter<T extends Keyable> {
    FilterFieldData<T> apply(int recordNumber, T keyable, FilterFieldDataDescriptor<T> descriptor);
}
