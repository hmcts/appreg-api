package uk.gov.hmcts.appregister.data.filter.meta;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;

/**
 * A function that generates a value for a filter descriptor.
 */
@FunctionalInterface
public interface GenerateAccordingToFilter<T extends Keyable> {

    /**
     * Creates a filter field data object according to the filter descriptor. The filter field data
     * should contain a value. That value needs to also be set on the keyable.
     *
     * @param recordNumber The record number of the filter field data.
     * @param keyable The keyable to apply the filter to.
     * @param descriptor The filter descriptor that relates to a filter field.
     * @return The filter field data that is generated according to the filter descriptor.
     */
    FilterFieldData<T> apply(
            int recordNumber, T keyable, FilterFieldDataMetaDescriptor<T> descriptor);
}
