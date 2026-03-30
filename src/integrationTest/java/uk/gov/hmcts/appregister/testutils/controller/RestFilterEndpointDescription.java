package uk.gov.hmcts.appregister.testutils.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;

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

    /** The url to call. */
    private URL url;

    public RestFilterEndpointDescription(RestFilterEndpointDescription<T> description) {
        setUrl(description.getUrl());
        filterableScenario = description.filterableScenario;
        sortDescriptors = description.sortDescriptors;
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
