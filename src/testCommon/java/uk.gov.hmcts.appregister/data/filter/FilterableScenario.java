package uk.gov.hmcts.appregister.data.filter;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.ArrayList;
import java.util.List;

/**
 * A filterable scenario that support finding the keyable data based on the filter values.
 */
@Getter
@Setter
public class FilterableScenario {

    /** The start of the scenario. */
    private List<FilterFieldData> startData;

    /** The end of the scenario. */
    private List<FilterFieldData> endData;

    public FilterableScenario() {}

    /**
     * gets all combinations of the filters.
     * @return A combination of filterable data values
     */
    public List<FilterableScenario> getAllCombinations() {
        List<FilterableScenario> result = new ArrayList<>();

        int n = getOrderFilterValues(OrderEnum.START).size();
        int total = 1 << n; // 2^n

        for (int mask = 0; mask < total; mask++) {
            FilterableScenario subset = new FilterableScenario();

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.startData.add(getOrderFilterValues(OrderEnum.START).get(i));
                    subset.endData.add(getOrderFilterValues(OrderEnum.END).get(i));
                }
            }

            result.add(subset);
        }

        return result;
    }

    /**
     * gets the partial value for a descriptor
     * @param descriptor The descriptor to search for.
     */
    public FilterValue getValue(
        FilterFieldDataDescriptor descriptor, OrderEnum orderEnum) {
        for (FilterFieldData filterFieldData : getOrderFilterValues(orderEnum)) {
            if (descriptor == filterFieldData.getDescriptor()) {
                return filterFieldData.getKeyableValues();
            }
        }
        return null;
    }

    private List<FilterFieldData> getOrderFilterValues(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return startData;
        } else {
            return endData;
        }
    }

}
