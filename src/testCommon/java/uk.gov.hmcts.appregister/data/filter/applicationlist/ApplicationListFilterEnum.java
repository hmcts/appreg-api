package uk.gov.hmcts.appregister.data.filter.applicationlist;

import java.time.LocalDate;
import java.time.LocalTime;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldValue;
import uk.gov.hmcts.appregister.data.filter.generator.FilterFieldDataGenerator;
import uk.gov.hmcts.appregister.data.filter.generator.PrimitiveDataGenerator;
import uk.gov.hmcts.appregister.data.filter.meta.FilterFieldDataMetaDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;

/**
 * An enumeration that allows us to setup filter for the application list endpoint.
 */
public enum ApplicationListFilterEnum implements FilterMetaDescriptorEnum<ApplicationList> {
    DATE(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("date")
                    .partialSupport(false)
                    .caseInsensitive(false)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                LocalDate date = PrimitiveDataGenerator.getDate(count);
                                FilterFieldData<ApplicationList> filterFieldData =
                                        new FilterFieldData<>();
                                FilterFieldValue<ApplicationList> value = new FilterFieldValue<>();
                                value.setKeyable(keyable);
                                value.setValue(date);
                                filterFieldData.setKeyableValues(value);
                                filterFieldData.setDescriptor(descriptor);
                                keyable.setDate(PrimitiveDataGenerator.getDate(count));
                                return filterFieldData;
                            })
                    .build()),
    TIME(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("time")
                    .partialSupport(false)
                    .caseInsensitive(false)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                LocalTime time = PrimitiveDataGenerator.getTime(count);
                                FilterFieldData<ApplicationList> filterFieldData =
                                        new FilterFieldData<>();
                                FilterFieldValue<ApplicationList> value = new FilterFieldValue<>();
                                value.setKeyable(keyable);
                                value.setValue(time);
                                filterFieldData.setKeyableValues(value);
                                filterFieldData.setDescriptor(descriptor);
                                keyable.setTime(time);
                                return filterFieldData;
                            })
                    .build()),
    COURT_LOCATION_CODE(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("courtLocationCode")
                    .partialSupport(false)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationList> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 10);
                                keyable.setCourtCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    CJA_CODE(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("cjaCode")
                    .partialSupport(false)
                    .caseInsensitive(false)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationList> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 2);

                                CriminalJusticeTestData criminalJusticeTestData =
                                        new CriminalJusticeTestData();
                                CriminalJusticeArea criminalJusticeArea =
                                        criminalJusticeTestData.someComplete();
                                criminalJusticeArea.setCode(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                keyable.setCja(criminalJusticeArea);
                                return filterFieldData;
                            })
                    .build()),
    DESCRIPTION(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("description")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationList> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 10);
                                keyable.setDescription(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    OTHER_LOCATION_DESCRIPTION(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("otherLocationDescription")
                    .partialSupport(true)
                    .caseInsensitive(true)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                FilterFieldData<ApplicationList> filterFieldData =
                                        FilterFieldDataGenerator.getFieldDataWithString(
                                                count, descriptor, keyable, 10);
                                keyable.setOtherLocation(
                                        filterFieldData.getKeyableValues().getValue().toString());
                                return filterFieldData;
                            })
                    .build()),
    STATUS(
            FilterFieldDataMetaDescriptor.<ApplicationList>builder()
                    .queryName("status")
                    .partialSupport(false)
                    .caseInsensitive(false)
                    .filterGenerator(
                            (count, keyable, descriptor) -> {
                                Status status = Status.OPEN;
                                FilterFieldData<ApplicationList> filterFieldData =
                                        new FilterFieldData<>();
                                FilterFieldValue<ApplicationList> value = new FilterFieldValue<>();
                                value.setKeyable(keyable);
                                value.setValue(status);
                                filterFieldData.setKeyableValues(value);
                                filterFieldData.setDescriptor(descriptor);
                                keyable.setStatus(status);
                                return filterFieldData;
                            })
                    .build());

    private FilterFieldDataMetaDescriptor filterFieldDataDescriptor;

    ApplicationListFilterEnum(FilterFieldDataMetaDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;
    }

    @Override
    public FilterFieldDataMetaDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
