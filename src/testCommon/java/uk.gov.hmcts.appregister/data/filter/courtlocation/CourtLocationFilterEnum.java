package uk.gov.hmcts.appregister.data.filter.courtlocation;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.value.AbstractFilterGenerator;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum CourtLocationFilterEnum implements FilterDescriptionEnum<NationalCourtHouse> {

    CODE(
        FilterFieldDataDescriptor.<NationalCourtHouse>builder()
            .queryName("code")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((keyable, descriptor, orderEnum) -> {
                FilterFieldData<NationalCourtHouse> filterFieldData = FilterUtil.getFieldData(10, descriptor, keyable);
                keyable.setCourtLocationCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    ),
    NAME(
        FilterFieldDataDescriptor.<NationalCourtHouse>builder()
            .queryName("name")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((keyable, descriptor, orderEnum) -> {
                FilterFieldData<NationalCourtHouse> filterFieldData = FilterUtil.getFieldData(50, descriptor, keyable);
                keyable.setName(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    );

    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    CourtLocationFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
