package uk.gov.hmcts.appregister.data.filter.standardapplicant;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum StandardApplicantFilterEnum implements FilterDescriptionEnum<StandardApplicant> {

    CODE(
        FilterFieldDataDescriptor.<StandardApplicant>builder()
            .queryName("code")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<StandardApplicant> filterFieldData = FilterUtil.getFieldDataWithString(count, descriptor, keyable, 2);
                keyable.setApplicantCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    ),
    NAME(
        FilterFieldDataDescriptor.<StandardApplicant>builder()
            .queryName("name")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<StandardApplicant> filterFieldData = FilterUtil.getFieldDataWithString(count,  descriptor, keyable, 35);
                keyable.setName(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    );

    private FilterFieldDataDescriptor<StandardApplicant> filterFieldDataDescriptor;

    StandardApplicantFilterEnum(FilterFieldDataDescriptor<StandardApplicant> filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor<StandardApplicant> getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
