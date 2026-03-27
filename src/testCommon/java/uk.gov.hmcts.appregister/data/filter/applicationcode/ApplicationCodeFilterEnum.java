package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.value.AbstractFilterGenerator;
import uk.gov.hmcts.appregister.data.filter.value.GenerateAccordingToFilter;

import java.util.Random;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ApplicationCodeFilterEnum implements FilterDescriptionEnum<ApplicationCode> {

    CODE(
        FilterFieldDataDescriptor.<ApplicationCode>builder()
            .queryName("code")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((keyable, descriptor, orderEnum) -> {
                FilterFieldData<ApplicationCode> filterFieldData = FilterUtil.getFieldData(10, descriptor, keyable);
                keyable.setCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    ),
    TITLE(
        FilterFieldDataDescriptor.<ApplicationCode>builder()
            .queryName("title")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(new AbstractFilterGenerator<ApplicationCode>() {
                @Override
                protected void setValue(String obj, FilterValue<ApplicationCode> value, FilterFieldDataDescriptor<ApplicationCode> descriptor,  OrderEnum orderEnum) {
                    value.getKeyable().setTitle(obj);
                }
            })
            .build()
    );

    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    ApplicationCodeFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
