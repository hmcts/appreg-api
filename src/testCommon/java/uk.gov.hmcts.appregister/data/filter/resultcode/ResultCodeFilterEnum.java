package uk.gov.hmcts.appregister.data.filter.resultcode;

import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup filtering for the result code endpoint.
 */
public enum ResultCodeFilterEnum implements FilterMetaDescriptorEnum<ResolutionCode> {
    CODE(
            FilterFieldDataMetaDescriptor.<ResolutionCode>builder()
                    .queryName("code")
                    .partialSupport(false)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ResolutionCode> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 2);
                                keyable.setResultCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    NAME(
            FilterFieldDataMetaDescriptor.<ResolutionCode>builder()
                    .queryName("title")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ResolutionCode> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 35);
                                keyable.setTitle(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build());

    private FilterFieldDataMetaDescriptor filterFieldDataDescriptor;

    ResultCodeFilterEnum(FilterFieldDataMetaDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
