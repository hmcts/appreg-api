package uk.gov.hmcts.appregister.data.filter.resultcode;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ResultCodeFilterEnum implements FilterDescriptionEnum<ResolutionCode> {

    CODE(
        FilterFieldDataDescriptor.<ResolutionCode>builder()
            .queryName("code")
            .partialSupport(false)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ResolutionCode> filterFieldData = FilterUtil.getFieldDataWithString(count, descriptor, keyable, 2);
                keyable.setResultCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    ),
    NAME(
        FilterFieldDataDescriptor.<ResolutionCode>builder()
            .queryName("title")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ResolutionCode> filterFieldData = FilterUtil.getFieldDataWithString(count,  descriptor, keyable, 35);
                keyable.setTitle(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    );

    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    ResultCodeFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
