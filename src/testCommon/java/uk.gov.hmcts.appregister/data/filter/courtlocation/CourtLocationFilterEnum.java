package uk.gov.hmcts.appregister.data.filter.courtlocation;

import uk.gov.hmcts.appregister.common.entity.NationalCourtHouse;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup filter for the court justice endpoint.
 */
public enum CourtLocationFilterEnum implements FilterMetaDescriptorEnum<NationalCourtHouse> {
    CODE(
            FilterFieldDataMetaDescriptor.<NationalCourtHouse>builder()
                    .queryName("code")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<NationalCourtHouse> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 10);
                                keyable.setCourtLocationCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    NAME(
            FilterFieldDataMetaDescriptor.<NationalCourtHouse>builder()
                    .queryName("name")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<NationalCourtHouse> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 50);
                                keyable.setName(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build());

    private FilterFieldDataMetaDescriptor filterFieldDataDescriptor;

    CourtLocationFilterEnum(FilterFieldDataMetaDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
