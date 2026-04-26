package uk.gov.hmcts.appregister.filter;

import com.github.tomakehurst.wiremock.stubbing.Scenario;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An essential filter scenario strategy that generates come filter scenarios.
 *
 * 1) All filters
 * 2) Randomised set of 4 filters
 */
public class EssentialFilterScenarioStrategy implements FilterScenarioStrategy {
    private static final int NUMBER_OF_FILTERS_TO_APPLY = 4;

    @Override
    public <T extends Keyable> List<FilterableScenario<T>> getScenarioCombinations(FilterableScenario<T> scenario) {

        List<FilterableScenario<T>> oneScenario = new ArrayList<>();

        for (int dataCount = 0; dataCount < scenario.getFilterData().getFirst().size(); dataCount++) {
            FilterableScenario<T> scenarioNew = new FilterableScenario<>();

            for (int i = 0; i < scenario.getFilterData().size(); i++) {
                List<FilterFieldData<T>> filterFieldDataLst = new ArrayList<>();
                filterFieldDataLst.add(scenario.getFilterData().get(i).get(dataCount).deepClone());
                scenarioNew.getFilterData().add(filterFieldDataLst);
            }
            oneScenario.add(scenarioNew);
        }

        // now generate a scenario with random filter fields
        if (scenario.getFilterData().getFirst().size() > NUMBER_OF_FILTERS_TO_APPLY) {
            List<Integer> randomNumbers
                = getRandomNumbers(scenario.getFilterData().getFirst().size());

            FilterableScenario<T> scenarioNew = new FilterableScenario<>();

            // copy the scenario data and apply the randomised filter fields
            for (List<FilterFieldData<T>> data : scenario.getFilterData()) {
                List<FilterFieldData<T>> copy = copyScenarioRow(data, randomNumbers);
                scenarioNew.add(copy);
            }

            // run all filters and a sub set based on two filter scenarios
            ArrayList<FilterableScenario<T>> resultingScenarios = new ArrayList<>(oneScenario);
            resultingScenarios.add(scenario);
            resultingScenarios.add(scenarioNew);

            return resultingScenarios;
        }

        return List.of(scenario);
    }

    private <T extends Keyable> List<FilterFieldData<T>> copyScenarioRow(List<FilterFieldData<T>> dataToCopy,
                                                                         List<Integer> randomNumbersToCopy) {
        List<FilterFieldData<T>> result = new ArrayList<>();

        // loop through each filter row. Lets select the fields we want to search on
        for (Integer data : randomNumbersToCopy) {
            FilterFieldData<T> copy = dataToCopy.get(data - 1).deepClone();

            // copy the data of a row of data in the scenario
            if (result.size() == 0) {
                result.add(copy);
            } else {
                // always ensure that the same keyable is used across all values in the row
                copy.getKeyableValues().setKeyable(result.getFirst()
                                                       .getKeyableValues().getKeyable());
                result.add(copy);
            }
        }

        return result;
    }

    private List<Integer> getRandomNumbers(int max) {
        int min = 1;

        List<Integer> numbers = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            numbers.add(i);
        }

        Collections.shuffle(numbers);

        return numbers.subList(0, NUMBER_OF_FILTERS_TO_APPLY);
    }
}
