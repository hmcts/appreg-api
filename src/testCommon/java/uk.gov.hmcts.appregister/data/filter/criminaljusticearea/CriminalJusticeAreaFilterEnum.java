package uk.gov.hmcts.appregister.data.filter.criminaljusticearea;

import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum CriminalJusticeAreaFilterEnum implements FilterDescriptionEnum<CriminalJusticeArea> {

    CODE(
        FilterFieldDataDescriptor.<CriminalJusticeArea>builder()
            .queryName("code")
            .partialSupport(false)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<CriminalJusticeArea> filterFieldData = FilterUtil.getFieldDataWithString(count, descriptor, keyable, 2);
                keyable.setCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    ),
    NAME(
        FilterFieldDataDescriptor.<CriminalJusticeArea>builder()
            .queryName("description")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<CriminalJusticeArea> filterFieldData = FilterUtil.getFieldDataWithString(count,  descriptor, keyable, 35);
                keyable.setDescription(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()
    );

    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    CriminalJusticeAreaFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
