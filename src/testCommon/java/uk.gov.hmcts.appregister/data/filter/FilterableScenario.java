package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.applicationcode.ApplicationCodeFilterEnum;
import uk.gov.hmcts.appregister.util.CopyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A filterable scenario that support finding the keyable data based on the filter values.
 */
@Getter
@Setter
public class FilterableScenario<T extends Keyable> {

    /** The start of the scenario. */
    private List<FilterFieldData<T>> startData = new ArrayList<>();

    /** The end of the scenario. */
    private List<FilterFieldData<T>> endData = new ArrayList<>();

    public FilterableScenario() {}

    /**
     * gets all combinations of the filters.
     * @return A combination of filterable data values
     */
    public List<FilterableScenario<T>> getAllCombinations() {
        List<FilterableScenario<T>> result = new ArrayList<>();

        int n = getOrderFilterValues(OrderEnum.START).size();
        int total = 1 << n; // 2^n

        for (int mask = 0; mask < total; mask++) {
            FilterableScenario<T> subset = new FilterableScenario<T>();

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.startData.add(getOrderFilterValues(OrderEnum.START).get(i).deepClone());
                    subset.endData.add(getOrderFilterValues(OrderEnum.END).get(i).deepClone());
                }
            }

            // do not add scenario where we have no filter criteria
            if (!subset.getStartData().isEmpty() &&
                !subset.getEndData().isEmpty()) {
                result.add(subset);
            }
        }

        return result;
    }

    /**
     * gets the partial value for a descriptor
     * @param descriptor The descriptor to search for.
     */
    public FilterValue<T> getValue(
        FilterFieldDataDescriptor descriptor, OrderEnum orderEnum) {
        for (FilterFieldData<T> filterFieldData : getOrderFilterValues(orderEnum)) {
            if (descriptor == filterFieldData.getDescriptor()) {
                return filterFieldData.getKeyableValues();
            }
        }
        return null;
    }

    /**
     * gets the keyable values for the start and end of the scenario.
     * @return The keyable values for the start and end of the scenario
     */
    public List<T> getAllKeyable() {
        List<T> result = new ArrayList<>();
        result.add(startData.getFirst().getKeyableValues().getKeyable());
        result.add(endData.getFirst().getKeyableValues().getKeyable());
        return result;
    }

    private List<FilterFieldData<T>> getOrderFilterValues(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return startData;
        } else {
            return endData;
        }
    }
}
