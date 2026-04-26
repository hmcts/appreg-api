package uk.gov.hmcts.appregister.filter;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.ArrayList;
import java.util.List;

/**
 * A strategy that allows us to get a combination of filter scenarios. This
 * can only be used when time is not an issue. This strategy takes a LOOOOONNNNGGG time
 * to complete.
 */
public class AllFilterCombinationScenarioStrategy implements FilterScenarioStrategy {

    /**
     * gets all combinations of the filters. This is vital to test all possible combinations of
     * filter query values.
     *
     * @return This will generate 2^n combinations where n is the number of filter field data values
     *     in the first record of filter data. Each combination will be a subset of the original
     *     filter data.
     */
    public <T extends Keyable> List<FilterableScenario<T>> getScenarioCombinations(FilterableScenario<T> scenario) {
        List<FilterableScenario<T>> result = new ArrayList<>();

        int n = scenario.getFilterData().getFirst().size();
        int total = 1 << n; // 2^n

        for (int mask = 0; mask < total; mask++) {
            FilterableScenario<T> scenarioNew = new FilterableScenario<T>();

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    for (int j = 0; j < scenario.getFilterData().size(); j++) {
                        List<FilterFieldData<T>> filterFieldDataLst;
                        if (j > (scenarioNew.getFilterData().size() - 1)) {
                            filterFieldDataLst = new ArrayList<>();
                            scenarioNew.getFilterData().add(filterFieldDataLst);
                        } else {
                            filterFieldDataLst = scenario.getFilterData().get(j);
                        }

                        // clone the first filter field data and set it as the keyable value
                        if (filterFieldDataLst.isEmpty()) {
                            filterFieldDataLst.add(scenario.getFilterData().get(j).get(i).deepClone());
                        } else {
                            FilterFieldData<T> filterFieldValue = scenario.getFilterData().get(j).get(i).deepClone();

                            // use the same cloned value as the first entry for all subsequent entries
                            filterFieldValue.getKeyableValues().setKeyable(filterFieldDataLst.get(0).getKeyableValues().getKeyable());

                            filterFieldDataLst.add(filterFieldValue);
                        }
                    }
                }
            }
            result.add(scenario);
        }

        return result;
    }

}
