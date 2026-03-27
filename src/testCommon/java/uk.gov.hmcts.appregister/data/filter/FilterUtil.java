package uk.gov.hmcts.appregister.data.filter;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;

import java.util.Random;

public class FilterUtil {

    public static <T extends Keyable> FilterFieldData<T> getFieldData(Integer max,
                                                                  FilterFieldDataDescriptor<T>
                                            filterFieldDataDescriptor, T keyable) {
        Random random = new Random();

        if (filterFieldDataDescriptor.isPartialSupport()) {
            int randomNumber = random.nextInt(max - 3);

            // fail fast if we can not generate a string within the limits
            if (randomNumber < 0) {
                throw new FilterProcessingException(randomNumber + " cannot be less than 0");
            }

            String partialstr = "S" + randomNumber + "M" + randomNumber + "E" + randomNumber;

            FilterValue<T> filterValue = new FilterValue<T>(keyable, partialstr);

            PartialFilterData<T> filterData = new PartialFilterData<>();
            filterValue.setValue(partialstr);

            filterData.setStartsWith("S" + randomNumber);
            filterData.setMiddleWith("M" + randomNumber);
            filterData.setEndsWith("E" + randomNumber);
            filterData.setDescriptor(filterFieldDataDescriptor);
            filterData.setMatchOnAllPartials("S");
            filterData.setKeyableValues(filterValue);

            return filterData;
        } else {
            int randomNumber = random.nextInt(max - 1);

            String randomStr = "V" + randomNumber;

            // fail fast if we can not generate a string within the limits
            if (randomNumber < 0) {
                throw new FilterProcessingException(randomNumber + " cannot be less than 0");
            }

            FilterValue<T> filterValue = new FilterValue<T>(keyable, randomStr);
            FilterFieldData<T> filterFieldData = new FilterFieldData<>();
            filterFieldData.setKeyableValues(filterValue);
            filterFieldData.setDescriptor(filterFieldDataDescriptor);
            filterFieldData.setKeyableValues(filterValue);

            return filterFieldData;
        }
    }
}
