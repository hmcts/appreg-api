package uk.gov.hmcts.appregister.testutils.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.filter.FilterFieldData;
import uk.gov.hmcts.appregister.filter.FilterScenarioFactory;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.meta.SortMetaDescriptorEnum;

/**
 * Represents a filter endpoint description.
 */
@Getter
@RequiredArgsConstructor
@Setter
public class RestFilterEndpointDescription<T extends Keyable> {

    /** The filterable scenario containing the filter data. */
    private FilterableScenario<T> filterableScenario;

    /** The sort descriptors that can be used to sort the results along side the filter. */
    private List<SortMetaDescriptorEnum<T>> sortDescriptors;

    @FunctionalInterface
    public interface GetUrlFunction<T extends Keyable> {
        URL getUrl(T keyable) throws IOException;
    }

    /**
     * gets the url that we need to call for this filter test. Used to get the url to make the rest
     * call for the filter.
     */
    private GetUrlFunction<T> getUrlFunction;

    public RestFilterEndpointDescription(RestFilterEndpointDescription<T> description) {
        filterableScenario = description.filterableScenario;
        sortDescriptors = description.sortDescriptors;
        getUrlFunction = description.getUrlFunction;
    }

    /**
     * gets all of the rest filter descriptions for a given scenario.
     *
     * @param scenario The scenario to get the rest filter descriptions for.
     * @return All of the rest filter descriptions for a given scenario.
     */
    public List<RestFilterEndpointDescription<T>> getForScenario(FilterableScenario<T> scenario) {
        List<RestFilterEndpointDescription<T>> restFilterDescriptionsLst = new ArrayList<>();
        for (FilterableScenario<T> filterableScenario : scenario.getAllCombinations()) {
            RestFilterEndpointDescription<T> restFilterCopy =
                    new RestFilterEndpointDescription<T>(this);
            restFilterCopy.filterableScenario = filterableScenario;

            if (restFilterCopy.filterableScenario.getFilterData().size() != 0) {
                restFilterDescriptionsLst.add(restFilterCopy);
            }
        }

        return restFilterDescriptionsLst;
    }

    @Override
    public String toString() {
        return "Filtering using query parameters "
                + filterableScenario.getFilterData().getFirst().stream()
                        .map(filterFieldData -> filterFieldData.getDescriptor().getQueryName())
                        .toList();
    }
}
