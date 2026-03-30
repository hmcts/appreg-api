package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.sort.SortDescriptorEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * A filterable scenario that support finding the keyable data based on the filter values.
 */
@Getter
@Setter
public class FilterableScenario<T extends Keyable> {

    /**
     * The start of the scenario.
     */
    private List<List<FilterFieldData<T>>> filterData = new ArrayList<>();

    private List<SortDescriptorEnum<T>> sortDescriptorEnums = new ArrayList<>();

    public FilterableScenario() {
    }

    public void add(List<FilterFieldData<T>> filterFieldData) {
        this.filterData.add(filterFieldData);
    }

    /**
     * gets all combinations of the filters.
     *
     * @return A combination of filterable data values
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
     * gets the keyable values for the start and end of the scenario.
     *
     * @return The keyable values for the start and end of the scenario
     */
    public List<T> getAllKeyable() {
        List<T> result = new ArrayList<>();
        for (List<FilterFieldData<T>> filterFieldData : filterData) {
            result.add(filterFieldData.getFirst().getKeyableValues().getKeyable());
        }
        return result;
    }
}
