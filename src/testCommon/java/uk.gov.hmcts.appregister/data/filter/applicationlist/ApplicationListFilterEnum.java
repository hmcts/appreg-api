package uk.gov.hmcts.appregister.data.filter.applicationlist;

import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.data.CriminalJusticeTestData;
import uk.gov.hmcts.appregister.data.filter.FilterDescriptionEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.FilterUtil;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.PrimitiveDataGenerator;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * An enumeration that allows us to setup filtering for the application code endpoint.
 */
public enum ApplicationListFilterEnum implements FilterDescriptionEnum<ApplicationList> {

    DATE(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("date")
            .partialSupport(false)
            .caseInsensitive(false)
            .filterGenerator((count, keyable, descriptor) -> {
                LocalDate date = PrimitiveDataGenerator.getDate(count);
                FilterFieldData<ApplicationList> filterFieldData = new
                    FilterFieldData<>();
                FilterValue<ApplicationList> value = new FilterValue<>();
                value.setKeyable(keyable);
                value.setValue(date);
                filterFieldData.setKeyableValues(value);
                filterFieldData.setDescriptor(descriptor);
                keyable.setDate(PrimitiveDataGenerator.getDate(count));
                return filterFieldData;
            })
            .build()
    ),
    TIME(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("time")
            .partialSupport(false)
            .caseInsensitive(false)
            .filterGenerator((count, keyable, descriptor) -> {
                LocalTime time = PrimitiveDataGenerator.getTime(count);
                FilterFieldData<ApplicationList> filterFieldData = new
                    FilterFieldData<>();
                FilterValue<ApplicationList> value = new FilterValue<>();
                value.setKeyable(keyable);
                value.setValue(time);
                filterFieldData.setKeyableValues(value);
                filterFieldData.setDescriptor(descriptor);
                keyable.setTime(time);
                return filterFieldData;
            })
            .build()),
    COURT_LOCATION_CODE(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("courtLocationCode")
            .partialSupport(false)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ApplicationList> filterFieldData = FilterUtil
                    .getFieldDataWithString(count, descriptor, keyable, 10);
                keyable.setCourtCode(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()),
    CJA_CODE(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("cjaCode")
            .partialSupport(false)
            .caseInsensitive(false)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ApplicationList> filterFieldData = FilterUtil
                    .getFieldDataWithString(count, descriptor, keyable, 2);

                CriminalJusticeTestData criminalJusticeTestData = new CriminalJusticeTestData();
                CriminalJusticeArea criminalJusticeArea = criminalJusticeTestData.someComplete();
                criminalJusticeArea.setCode(filterFieldData.getKeyableValues().getValue().toString());
                keyable.setCja(criminalJusticeArea);
                return filterFieldData;
            })
            .build()),
    DESCRIPTION(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("description")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ApplicationList> filterFieldData = FilterUtil
                    .getFieldDataWithString(count, descriptor, keyable, 10);
                keyable.setDescription(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()),
    OTHER_LOCATION_DESCRIPTION(
        FilterFieldDataDescriptor.<ApplicationList>builder()
            .queryName("otherLocationDescription")
            .partialSupport(true)
            .caseInsensitive(true)
            .filterGenerator((count, keyable, descriptor) -> {
                FilterFieldData<ApplicationList> filterFieldData = FilterUtil
                    .getFieldDataWithString(count, descriptor, keyable, 10);
                keyable.setOtherLocation(filterFieldData.getKeyableValues().getValue().toString());
                return filterFieldData;
            })
            .build()),
        STATUS(
            FilterFieldDataDescriptor.<ApplicationList>builder()
                .queryName("status")
                .partialSupport(false)
                .caseInsensitive(false)
                .filterGenerator((count, keyable, descriptor) -> {
                    Status status = Status.OPEN;
                    FilterFieldData<ApplicationList> filterFieldData = new
                        FilterFieldData<>();
                    FilterValue<ApplicationList> value = new FilterValue<>();
                    value.setKeyable(keyable);
                    value.setValue(status);
                    filterFieldData.setKeyableValues(value);
                    filterFieldData.setDescriptor(descriptor);
                    keyable.setStatus(status);
                    return filterFieldData;
                })
                .build());

    private FilterFieldDataDescriptor filterFieldDataDescriptor;

    ApplicationListFilterEnum(FilterFieldDataDescriptor filterFieldDataDescriptor) {
        this.filterFieldDataDescriptor = filterFieldDataDescriptor;

    }

    @Override
    public FilterFieldDataDescriptor getDescriptor() {
        return filterFieldDataDescriptor;
    }
}
