package uk.gov.hmcts.appregister.data.filter;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.value.AbstractValueGenerator;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ApplicationCodeFilterEnum implements FilterDescriptionEnum{

    CODE(
        FilterFieldDataDescriptor.builder()
            .queryName("code")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(new AbstractValueGenerator() {
                @Override
                protected void setValue(String obj, FilterValue value, FilterFieldDataDescriptor descriptor) {
                    ((ApplicationCode)value.getKeyable()).setCode(obj);
                }
            })
            .build()
    ),
    TITLE(
        FilterFieldDataDescriptor.builder()
            .queryName("title")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator(new AbstractValueGenerator() {
                @Override
                protected void setValue(String obj, FilterValue value, FilterFieldDataDescriptor descriptor) {
                    ((ApplicationCode)value.getKeyable()).setCode(obj);
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
