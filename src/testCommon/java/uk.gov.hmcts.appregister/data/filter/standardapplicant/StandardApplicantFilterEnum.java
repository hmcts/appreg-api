package uk.gov.hmcts.appregister.data.filter.standardapplicant;

import uk.gov.hmcts.appregister.common.entity.StandardApplicant;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup filtering for the standard applicant.
 */
public enum StandardApplicantFilterEnum implements FilterMetaDescriptorEnum<StandardApplicant> {
    CODE(
            FilterFieldDataMetaDescriptor.<StandardApplicant>builder()
                    .queryName("code")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<StandardApplicant> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 2);
                                keyable.setApplicantCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    NAME(
            FilterFieldDataMetaDescriptor.<StandardApplicant>builder()
                    .queryName("name")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<StandardApplicant> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 35);
                                keyable.setName(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build());

    private FilterFieldDataMetaDescriptor<StandardApplicant> filterFieldDataDescriptor;

    StandardApplicantFilterEnum(
            FilterFieldDataMetaDescriptor<StandardApplicant> filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor<StandardApplicant> getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
