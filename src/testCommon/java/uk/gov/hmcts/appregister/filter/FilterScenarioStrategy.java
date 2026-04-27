package uk.gov.hmcts.appregister.filter;

import java.util.List;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

public interface FilterScenarioStrategy {
    /**
     * gets combinations= of the filters. This is vital to test all possible combinations of filter
     * query values.
     *
     * @param scenario The scenario with all filters to generate combinations for.
     * @return The scenarios that need to be executed
     */
    <T extends Keyable> List<FilterableScenario<T>> getScenarioCombinations(
            FilterableScenario<T> scenario);
}
