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
public abstract class AbstractValueGenerator implements GenerateAccordingToFilter {
    @Override
    public FilterFieldData apply(Keyable keyable, FilterFieldDataDescriptor descriptor, OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        FilterFieldData filterData = null;

        if (descriptor.isPartialSupport()) {
            String partialstr = "partialStart" + uuid + "partialMiddle" + uuid + "partialEnd" + uuid;
            FilterValue filterValue = new FilterValue(keyable, descriptor);
            filterData = PartialFilterData.builder()
                .startsWith("partialStart" + uuid)
                .middleWith("partialMiddle" + uuid)
                .endsWith("partialEnd" + uuid)
                .keyableValues(filterValue).build();
            setValue(partialstr, filterValue, descriptor);
        } else {
            FilterValue filterValue = new FilterValue(keyable, descriptor);
            setValue("val" + uuid, filterValue, descriptor);
        }

        return filterData;
    }

    protected abstract void setValue(String obj,
                                        FilterValue value,
                                        FilterFieldDataDescriptor descriptor);

    public String getString(OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        if (orderEnum == OrderEnum.START) {
            return "start " + uuid;
        } else {
            return "end " + uuid;
        }
    }

    public Boolean getBoolean(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return true;
        } else {
            return false;
        }
    }

    public LocalDate getDate(OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        if (orderEnum == OrderEnum.START) {
            return LocalDate.now();
        } else {
            return LocalDate.now().plusDays(2);
        }
    }

    public LocalTime getTime(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return LocalTime.now();
        } else {
            return LocalTime.now().plusHours(2);
        }
    }
}
