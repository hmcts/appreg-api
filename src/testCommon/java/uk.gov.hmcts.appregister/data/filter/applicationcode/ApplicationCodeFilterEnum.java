package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.OrderEnum;
import uk.gov.hmcts.appregister.data.filter.value.AbstractFilterGenerator;

import java.util.Random;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ApplicationCodeFilterEnum implements FilterDescriptionEnum {

    CODE(
        FilterFieldDataDescriptor.builder()
            .queryName("code")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(new AbstractFilterGenerator() {

                @Override
                protected void setValue(String obj, FilterValue value, FilterFieldDataDescriptor descriptor, OrderEnum orderEnum) {
                    Random random = new Random();
                    ((ApplicationCode) value.getKeyable()).setCode("Code" + random.nextInt(10));
                    value.setValue(((ApplicationCode) value.getKeyable()).getCode());
                }
            })
            .build()
    ),
    TITLE(
        FilterFieldDataDescriptor.builder()
            .queryName("title")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(new AbstractFilterGenerator() {
                @Override
                protected void setValue(String obj, FilterValue value, FilterFieldDataDescriptor descriptor,  OrderEnum orderEnum) {
                    ((ApplicationCode)value.getKeyable()).setTitle(obj);
                }
            })
            .build()
    ),
    ;


    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    ApplicationCodeFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
