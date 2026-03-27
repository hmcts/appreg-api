package uk.gov.hmcts.appregister.data.sort;

import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;

public interface SortDescriptorEnum<T> {
    SortDataDescriptor<T> getDescriptor();
}
