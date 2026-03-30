package uk.gov.hmcts.appregister.data.filter.sort;

public interface SortDescriptorEnum<T> {
    SortDataDescriptor<T> getDescriptor();
}
