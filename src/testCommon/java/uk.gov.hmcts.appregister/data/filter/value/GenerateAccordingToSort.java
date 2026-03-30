package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.data.filter.sort.SortDataDescriptor;

@FunctionalInterface
public interface GenerateAccordingToSort<T> {
    void apply(int recordNumber, T keyable, SortDataDescriptor<T> descriptor);
}
