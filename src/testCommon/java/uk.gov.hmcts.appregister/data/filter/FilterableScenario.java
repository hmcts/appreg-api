package uk.gov.hmcts.appregister.data.filter;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

/**
 * A filterable scenario that stores multiple filter scenarios.
 */
@Getter
@Setter
@NoArgsConstructor
public class FilterableScenario<T extends Keyable> {

    /**
     * The list of data that has been generated for this scenario. Each record of data has a set of
     * filter field data mapped against it.
     */
    private List<List<FilterFieldData<T>>> filterData = new ArrayList<>();

    /** The sort descriptor enums that relate to this filter. */
    private List<SortMetaDescriptorEnum<T>> sortDescriptorEnums = new ArrayList<>();

    public void add(List<FilterFieldData<T>> filterFieldData) {
        this.filterData.add(filterFieldData);
    }

    /**
     * gets all combinations of the filters. This is vital to test all possible combinations of
     * filter query values.
     *
     * @return This will generate 2^n combinations where n is the number of filter field data values
     *     in the first record of filter data. Each combination will be a subset of the original
     *     filter data.
     */
    public List<FilterableScenario<T>> getAllCombinations() {
        List<FilterableScenario<T>> result = new ArrayList<>();

        int n = filterData.getFirst().size();
        int total = 1 << n; // 2^n

        for (int mask = 0; mask < total; mask++) {
            FilterableScenario<T> scenario = new FilterableScenario<T>();
            scenario.setSortDescriptorEnums(sortDescriptorEnums);

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    for (int j = 0; j < filterData.size(); j++) {
                        List<FilterFieldData<T>> filterFieldDataLst;
                        if (j > (scenario.filterData.size() - 1)) {
                            filterFieldDataLst = new ArrayList<>();
                            scenario.filterData.add(filterFieldDataLst);
                        } else {
                            filterFieldDataLst = scenario.filterData.get(j);
                        }

                        filterFieldDataLst.add(filterData.get(j).get(i).deepClone());
                    }
                }
            }
            result.add(scenario);
        }

        return result;
    }

    /**
     * gets the keyable values for the scenario.
     *
     * @return The keyable values for the scenario.
     */
    public List<T> getAllKeyable() {
        List<T> result = new ArrayList<>();
        for (List<FilterFieldData<T>> filterFieldData : filterData) {
            result.add(filterFieldData.getFirst().getKeyableValues().getKeyable());
        }
        return result;
    }

    /**
     * is this scenario exclusively partial only.
     *
     * @return true if all the filter fields are partial only.
     */
    public boolean isPartialOnlyConfig() {
        return getFilterData().getFirst().stream()
                        .filter(data -> data instanceof PartialFilterFieldData)
                        .count()
                == getFilterData().getFirst().size();
    }

    /**
     * does this scenario have a partial filter data. returns true if there is at least one partial
     * filter data.
     */
    public boolean doesPartialExist() {
        return getFilterData().getFirst().stream()
                        .filter(data -> data instanceof PartialFilterFieldData)
                        .count()
                > 0;
    }
}
