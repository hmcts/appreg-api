package uk.gov.hmcts.appregister.testutils.controller;

import io.restassured.response.Response;
import lombok.Builder;
import lombok.Getter;

import lombok.experimental.SuperBuilder;

import org.springframework.http.HttpMethod;

import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.sort.SortDataDescriptor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuperBuilder
@Getter
public class RestFilterDescription extends RestEndpointDescription {
    private FilterableScenario filterableScenario;

    public RestFilterDescription(URL url, RoleEnum invalidRole,
                                 RoleEnum successRole, Object payload, Consumer<Response>
                                         responseConsumer,
                                 FilterableScenario filterableScenario) {
        super(url, HttpMethod.GET, invalidRole, successRole, payload, responseConsumer);
        this.filterableScenario = filterableScenario;
    }

    public RestFilterDescription(RestFilterDescription description) {
        super(description.getUrl(), HttpMethod.GET, description.getInvalidRole(), description.getSuccessRole(),
              description.getPayload(), description.getResponseConsumer());
        filterableScenario = description.filterableScenario;
    }

    /**
     * gets all of the rest filter descriptions for a given scenario.
     * @param scenario The scenario to get the rest filter descriptions for.
     * @return All of the rest filter descriptions for a given scenario.
     */
    public List<RestFilterDescription> getForScenario(FilterableScenario scenario) {
        List<RestFilterDescription> restFilterDescriptionsLst = new ArrayList<>();
        for (FilterableScenario filterableScenario : scenario.getAllCombinations()) {
            RestFilterDescription restFilterCopy = new RestFilterDescription(this);
            restFilterCopy.filterableScenario = filterableScenario;
            restFilterDescriptionsLst.add(restFilterCopy);
        }

        return restFilterDescriptionsLst;
    }
}
