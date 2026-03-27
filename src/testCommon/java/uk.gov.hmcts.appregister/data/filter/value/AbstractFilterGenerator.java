package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.PartialFilterData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A filter value generator that generates a value for a filter descriptor.
 */
public abstract class AbstractFilterGenerator<T extends Keyable> implements GenerateAccordingToFilter<T> {
    public AbstractFilterGenerator() {}

    @Override
    public FilterFieldData<T> apply(T keyable, FilterFieldDataDescriptor<T> descriptor, OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        PartialFilterData<T> filterData = null;

        if (descriptor.isPartialSupport()) {
            String partialstr;

            // make sure that we prefix an appropriate prefix to ensure sorting order
            partialstr = "partialStart" + uuid + "partialMiddle" + uuid + "partialEnd" + uuid;

            filterData = new PartialFilterData<T>();
            filterData.setStartsWith("partialStart" + uuid);
            filterData.setMiddleWith("partialMiddle" + uuid);
            filterData.setEndsWith("partialEnd" + uuid);
            filterData.setDescriptor(descriptor);
            filterData.setMatchOnAllPartials("partialStart");

            FilterValue<T> filterValue = new FilterValue<>(keyable, partialstr);
            setValue(partialstr, filterValue, descriptor, orderEnum);
            filterData.setKeyableValues(filterValue);
        } else {
            FilterValue<T> filterValue = new FilterValue<>(keyable, "val" + uuid);
            setValue("val" + uuid, filterValue, descriptor, orderEnum);

            filterData.setDescriptor(descriptor);
            filterData.setKeyableValues(filterValue);
        }

        return filterData;
    }

    protected abstract void setValue(String obj,
                                        FilterValue value,
                                        FilterFieldDataDescriptor descriptor, OrderEnum orderEnum);


}
