package uk.gov.hmcts.appregister.data.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.filter.meta.FilterMetaDescriptorEnum;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

/**
 * A filter factory that is designed to create filter scenarios.
 */
@RequiredArgsConstructor
public class FilterScenarioFactory {

    /** The number of records being generated within a filter scenario. */
    private static final int NUMBER_OF_RECORDS = 4;

    /**
     * creates the code using the full set of data descriptors. The scenario will be generated with
     * x records
     *
     * @param keyable The keyable (database entity) for this scenario. This allows us the means to
     *     generate baseline data.
     * @param filterDescriptionEnums The filter descriptors to use.
     * @param sortDescriptorEnums The sort descriptors to use.
     * @return The scenario
     */
    public static <T extends Keyable> FilterableScenario<T> createFilterScenario(
            T keyable,
            List<FilterMetaDescriptorEnum<T>> filterDescriptionEnums,
            List<SortMetaDescriptorEnum<T>> sortDescriptorEnums) {

        // validate the filter and sort enumerations
        assertFilterQueriesAreUnique(filterDescriptionEnums);
        assertSortIsUnique(sortDescriptorEnums);

        FilterableScenario<T> scenario = new FilterableScenario<T>();

        // loop through and generate unique records by deep cloning the keyable
        for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
            T copiedKey = (T) CopyUtil.deepClone(keyable);

            // loop through the baseline data and apply the filter data into the keyable
            List<FilterFieldData<T>> filterFieldDataLst = new ArrayList<>();
            for (int j = 0; j < filterDescriptionEnums.size(); j++) {
                filterFieldDataLst.add(
                        filterDescriptionEnums.get(j).getDescriptor().apply(i + 1, copiedKey));
            }
            scenario.add(filterFieldDataLst);
        }

        // add the sort descriptors
        scenario.setSortDescriptorEnums(sortDescriptorEnums);

        return scenario;
    }

    /**
     * creates a start and end keyable for sorting.
     *
     * @param keyable The keyable to use for the sort.
     * @param sortDescriptorEnums The sort to apply
     */
    public static <T extends Keyable> List<T> createSort(
            T keyable, List<SortMetaDescriptorEnum<T>> sortDescriptorEnums) {
        assertSortIsUnique(sortDescriptorEnums);

        List<T> result = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
            result.add(CopyUtil.deepClone(keyable));
            applySort(i + 1, result.getLast(), sortDescriptorEnums);
        }

        return result;
    }

    /**
     * apply all of the sort values to the keyable.
     *
     * @param count The number of the record.
     * @param keyable The keyable to apply the sort to
     * @param sortDescriptors The sort descriptor to apply data to the keyable
     */
    private static <T extends Keyable> void applySort(
            int count, T keyable, List<SortMetaDescriptorEnum<T>> sortDescriptors) {
        for (SortMetaDescriptorEnum<T> descriptorEnum : sortDescriptors) {
            descriptorEnum
                    .getDescriptor()
                    .getSortGenerator()
                    .apply(count, keyable, descriptorEnum.getDescriptor());
        }
    }

    /**
     * validates the filter descriptor is unique.
     *
     * @param filterDescriptionEnums The filter descriptor to validate
     */
    private static <T extends Keyable> void assertFilterQueriesAreUnique(
            List<FilterMetaDescriptorEnum<T>> filterDescriptionEnums) {
        Map<String, FilterMetaDescriptorEnum<T>> uniqueFilters = new HashMap<>();
        filterDescriptionEnums.forEach(
                en -> {
                    if (!uniqueFilters.containsKey(en.getDescriptor().getQueryName())) {
                        uniqueFilters.put(en.getDescriptor().getQueryName(), en);
                    } else {
                        throw new FilterProcessingException(
                                "Duplicate query name " + en.getDescriptor().getQueryName());
                    }
                });
    }

    /**
     * validates the sort descriptor is unique.
     *
     * @param sortDescriptorEnums The filter descriptor to validate
     */
    private static <T extends Keyable> void assertSortIsUnique(
            List<SortMetaDescriptorEnum<T>> sortDescriptorEnums) {
        Map<String, SortMetaDescriptorEnum<T>> uniqueFilters = new HashMap<>();
        sortDescriptorEnums.forEach(
                en -> {
                    if (!uniqueFilters.containsKey(
                            en.getDescriptor().getSortableOperationEnum().getApiValue())) {
                        uniqueFilters.put(
                                en.getDescriptor().getSortableOperationEnum().getApiValue(), en);
                    } else {
                        throw new FilterProcessingException(
                                "Duplicate sort name "
                                        + en.getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue());
                    }
                });
    }
}
