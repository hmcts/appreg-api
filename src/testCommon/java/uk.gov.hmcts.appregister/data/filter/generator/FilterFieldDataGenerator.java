package uk.gov.hmcts.appregister.data.filter.generator;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldValue;
import uk.gov.hmcts.appregister.data.filter.PartialFilterFieldData;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;

public class FilterFieldDataGenerator {

    /**
     * Generates a string filter field. If the descriptor supports partial values then we will
     * return a {link PartialFilterData} object. Otherwise we will return a {link FilterFieldData}.
     * The partial will be in the format e.g. S0M0E0 (.
     *
     * @param count The count
     * @param filterFieldDataDescriptor The filter descriptor to generate data
     * @param keyable The keyable to apply the filter to
     * @param max The maximum length of the string. This is only used if not a partial.
     * @return The filter field data mapping to the keyable and descriptor.
     */
    public static <T extends Keyable> FilterFieldData<T> getFieldDataWithString(
            int count,
            FilterFieldDataMetaDescriptor<T> filterFieldDataDescriptor,
            T keyable,
            Integer max) {

        // if the descriptor supports partial values then lets add some partials
        if (filterFieldDataDescriptor.isPartialSupport()) {
            String startStr = "S" + count + "M" + count;
            String endStr = "E" + count;
            String partialstr = startStr + endStr;

            int generateChars = max - (startStr.length() + endStr.length());

            // if the max is provided then we need to ensure that the partial string is not too long
            if (generateChars > 0) {
                partialstr = startStr + PrimitiveDataGenerator.generate(generateChars) + endStr;
            }

            FilterFieldValue<T> filterValue = new FilterFieldValue<T>(keyable, partialstr);

            PartialFilterFieldData<T> filterData = new PartialFilterFieldData<>();
            filterValue.setValue(partialstr);

            // add the partial filter data with the start, middle and end filters to return the
            // keyable value
            filterData.setStartsWith("S" + count);
            filterData.setMiddleWith("M" + count);
            filterData.setEndsWith("E" + count);
            filterData.setDescriptor(filterFieldDataDescriptor);

            // set a partial match for us to filter on
            filterData.setMatchOnAllPartials("M");
            filterData.setKeyableValues(filterValue);

            return filterData;
        } else {
            String randomStr = PrimitiveDataGenerator.generate(max);

            FilterFieldValue<T> filterValue = new FilterFieldValue<T>(keyable, randomStr);
            FilterFieldData<T> filterFieldData = new FilterFieldData<>();
            filterFieldData.setKeyableValues(filterValue);
            filterFieldData.setDescriptor(filterFieldDataDescriptor);
            filterFieldData.setKeyableValues(filterValue);

            return filterFieldData;
        }
    }
}
