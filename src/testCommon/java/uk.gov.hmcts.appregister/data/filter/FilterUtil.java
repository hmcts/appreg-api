package uk.gov.hmcts.appregister.data.filter;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;

import java.util.Random;

public class FilterUtil {

    /**
     * Generates a minimum size of 6.
     */
    public static <T extends Keyable> FilterFieldData<T> getFieldDataWithString(int count,
                                                                                FilterFieldDataDescriptor<T>
                                            filterFieldDataDescriptor, T keyable, Integer max) {
        if (filterFieldDataDescriptor.isPartialSupport()) {
            String partialstr = "S" + count + "M" + count + "E" + count;

            FilterValue<T> filterValue = new FilterValue<T>(keyable, partialstr);

            PartialFilterData<T> filterData = new PartialFilterData<>();
            filterValue.setValue(partialstr);

            filterData.setStartsWith("S" + count);
            filterData.setMiddleWith("M" + count);
            filterData.setEndsWith("E" + count);
            filterData.setDescriptor(filterFieldDataDescriptor);
            filterData.setMatchOnAllPartials("M");
            filterData.setKeyableValues(filterValue);

            return filterData;
        } else {
            String randomStr = PrimitiveDataGenerator.generate(max);

            FilterValue<T> filterValue = new FilterValue<T>(keyable, randomStr);
            FilterFieldData<T> filterFieldData = new FilterFieldData<>();
            filterFieldData.setKeyableValues(filterValue);
            filterFieldData.setDescriptor(filterFieldDataDescriptor);
            filterFieldData.setKeyableValues(filterValue);

            return filterFieldData;
        }
    }
}
