package uk.gov.hmcts.appregister.data.filter.applicationcode;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ApplicationCodeFilterEnum implements FilterMetaDescriptorEnum<ApplicationCode> {
    CODE(
            FilterFieldDataMetaDescriptor.<ApplicationCode>builder()
                    .queryName("code")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationCode> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 10);
                                keyable.setCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    TITLE(
            FilterFieldDataMetaDescriptor.<ApplicationCode>builder()
                    .queryName("title")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationCode> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 100);
                                keyable.setTitle(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build());

    private FilterFieldDataMetaDescriptor filterFieldDataDescriptor;

    ApplicationCodeFilterEnum(FilterFieldDataMetaDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
