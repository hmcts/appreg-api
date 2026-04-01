package uk.gov.hmcts.appregister.data.filter.meta;

/**
 * A function that generates a value for a sort descriptor.
 */
@FunctionalInterface
public interface GenerateAccordingToSort<T> {
    /**
     * Sets the sort data on the keyable according to the sort descriptor.
     *
     * @param recordNumber The record number of the filter field data.
     * @param keyable The keyable to apply the sort data to.
     * @param descriptor The sort descriptor that relates to the field to be sorted.
     */
    void apply(int recordNumber, T keyable, SortMetaDataDescriptor<T> descriptor);
}
