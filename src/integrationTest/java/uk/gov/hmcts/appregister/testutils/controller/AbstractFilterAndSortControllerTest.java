package uk.gov.hmcts.appregister.testutils.controller;

import io.restassured.response.Response;

import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.DatabaseReset;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A base class that allows for basic filter and sort testing.
 */
public abstract class AbstractFilterAndSortControllerTest extends BaseIntegration {

    @Autowired
    private DatabaseReset reset;

    @Autowired
    private DatabasePersistance persistance;

    /** The stream of negative security contexts to be tested. */
    protected abstract Stream<RestFilterDescription> getFilterSortableDescriptions() throws Exception;

    @Test
    public void runFilter(RestFilterDescription filterDescription) throws Exception {

        // save the start and end data to the database
        persistance.save(scenario.getStartData().getFirst().getKeyableValues().getKeyable());
        persistance.save(scenario.getEndData().getFirst().getKeyableValues().getKeyable());

        // filter using start data of scenario
        for (FilterFieldData filterFieldData :
            filterDescription.getFilterableScenario().getStartData()) {
            Response response = restAssuredClient.executeGetRequestWithPaging(
                Optional.of(0), Optional.of(1), null,
                filterDescription.getUrl(),
                getATokenWithValidCredentials().build().fetchTokenForRole(),
                req ->
                    applyQueryForStart(filterDescription, req)
            );

            Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());
            Assertions.assertEquals(
                response.asString(),
                mapToResponse(filterDescription
                                  .getFilterableScenario()
                                  .getStartData()
                                  .getFirst().getKeyableValues().getKeyable())
            );
        }
    }

    @Test
    public void runEachSort(RestFilterDescription filterSortableDescription) throws Exception {

    }

    /**
     * converts the keyable to the filter response
     * @param keyable the keyable to convert.
     * @return The object.
     */
    protected abstract String mapToResponse(Keyable keyable);

    /**
     * apply the query
     * @param filterSortableDescription the filter and sort description.
     * @param requestSpecification the request specification.
     */
    private RequestSpecification applyQueryForStart(RestFilterDescription filterSortableDescription,
                                                    RequestSpecification requestSpecification) {
        for (FilterFieldData scenario : filterSortableDescription
            .getFilterableScenario().getStartData()) {
            FilterValue filterValue = scenario.getKeyableValues()) {
                requestSpecification.queryParam(
                    filterValue.getDescriptor().getQueryName(),
                    filterValue.getValue()
                );
            }
        }
        return requestSpecification;
    }

}
