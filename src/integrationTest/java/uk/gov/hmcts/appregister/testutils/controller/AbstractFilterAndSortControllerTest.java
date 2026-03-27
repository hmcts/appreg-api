package uk.gov.hmcts.appregister.testutils.controller;

import io.restassured.response.Response;

import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import uk.gov.hmcts.appregister.common.entity.ApplicationCode;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterValue;
import uk.gov.hmcts.appregister.data.filter.PartialEnum;
import uk.gov.hmcts.appregister.data.filter.PartialFilterData;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.sort.KeyableSortComparator;
import uk.gov.hmcts.appregister.data.sort.SortDataDescriptor;
import uk.gov.hmcts.appregister.data.sort.SortDescriptorEnum;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A base class that allows for basic filter and sort testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractFilterAndSortControllerTest<T extends Keyable> extends BaseIntegration {
    @Autowired
    protected DatabasePersistance persistance;

    /**
     * The stream of negative security contexts to be tested.
     */
    protected abstract Stream<RestFilterEndpointDescription<T>> getFilterDescriptions() throws Exception;

    /**
     * The stream of negative security contexts to be tested.
     */
    protected abstract Stream<RestSortEndpointDescription<ApplicationCode>> getSortDescriptions() throws Exception;


    @BeforeEach
    public void setUp() throws Exception {
        Jwt jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runFilter(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        List<T> keyables = filterDescription.getFilterableScenario().getAllKeyable();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        // filter using the start data of the scenario
        for (FilterFieldData<T> filterFieldData :
            filterDescription.getFilterableScenario().getStartData()) {

            // if we have an instanceof of partial loop through all partial scenarios
            // match start, match middle, match end
            if (filterFieldData instanceof PartialFilterData == false) {
                runTest(filterDescription);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runFilterCaseInsensitive(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        List<T> keyables = filterDescription.getFilterableScenario().getAllKeyable();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        // filter using the start data of the scenario
        for (FilterFieldData<T> filterFieldData :
            filterDescription.getFilterableScenario().getStartData()) {

            if (filterFieldData instanceof PartialFilterData == false) {
                runTest(filterDescription, true);
            } else {
                runTestPartial(filterDescription, true);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runPartialFilter(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        List<T> keyables = filterDescription.getFilterableScenario().getAllKeyable();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        // filter using the start data of the scenario
        for (FilterFieldData<T> filterFieldData :
            filterDescription.getFilterableScenario().getStartData()) {

            // if we have an instanceof of partial loop through all partial scenarios
            // match start, match middle, match end
            if (filterFieldData instanceof PartialFilterData) {
                runTestPartial(filterDescription);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runPartialFilterGetAll(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        List<T> keyables = filterDescription.getFilterableScenario().getAllKeyable();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        if (isPartialOnlyConfig(filterDescription)) {
            Response response = runTest(
                filterDescription,
                req ->
                    applyQueryForStart(filterDescription, req, PartialEnum.ALL_PARTIALS_IN_SCENARIO),
                100
            );

            assertAllFilterWithDefaultSort(response, filterDescription);
        }
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runEachSortAscending(RestSortEndpointDescription<T> sortEndpointDescription) throws Exception {
        List<T> keyables = sortEndpointDescription.getExpectedToBeGenerated();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        Response response = restAssuredClient.executeGetRequestWithPaging(
            Optional.of(1), Optional.of(0), List.of(sortEndpointDescription.getSortDescriptors()
                                                        .getDescriptor().getSortableOperationEnum()
                                                        .getApiValue()
                                                        + "," + SortableField.ASC),
            sortEndpointDescription.getUrl(),
            getATokenWithValidCredentials().roles(List.of(RoleEnum.USER))
                .build().fetchTokenForRole()
        );

        List<T> keyable = sortEndpointDescription.getExpectedToBeGenerated();
        sortKeyables(keyable, sortEndpointDescription.getSortDescriptors().getDescriptor(), SortableField.ASC);
        Assertions.assertTrue(assertResponseInOrder(List.of(keyable.getFirst()), response, true));
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runEachSortDescending(RestSortEndpointDescription<T> sortEndpointDescription) throws Exception {
        List<T> keyables = sortEndpointDescription.getExpectedToBeGenerated();

        // save all keyable data that belongs to scenario
        keyables.stream().forEach(this::saveToDatabase);

        Response response = restAssuredClient.executeGetRequestWithPaging(
            Optional.of(1), Optional.of(0), List.of(sortEndpointDescription.getSortDescriptors()
                                                        .getDescriptor().getSortableOperationEnum().getApiValue()
                                                        + "," + SortableField.DESC),
            sortEndpointDescription.getUrl(),
            getATokenWithValidCredentials().roles(List.of(RoleEnum.USER))
                .build().fetchTokenForRole()
        );

        sortKeyables(keyables, sortEndpointDescription.getSortDescriptors().getDescriptor(),  SortableField.DESC);
        Assertions.assertTrue(assertResponseInOrder(List.of(keyables.getFirst()), response, true));
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runSortFailure(RestSortEndpointDescription<T> sortEndpointDescription) throws Exception {
        Response response = restAssuredClient.executeGetRequestWithPaging(
            Optional.of(1), Optional.of(0), List.of(sortEndpointDescription
                                                        .getSortDescriptors().getDescriptor()
                                                        .getSortableOperationEnum().getApiValue()
                                                        + "," + SortableField.DESC,
                                                        sortEndpointDescription
                                                            .getAvailableSortDescriptorsExcludingActive()
                                                            .get(0).getDescriptor().getSortableOperationEnum()
                                                            .getApiValue()  + "," + SortableField.ASC
                                                        ),
            sortEndpointDescription.getUrl(),
            getATokenWithValidCredentials().roles(List.of(RoleEnum.USER))
                .build().fetchTokenForRole()
        );

        ProblemAssertUtil.assertEquals(CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode(),
                                       response);
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runSortUnknown(RestSortEndpointDescription<T> sortEndpointDescription) throws Exception {
        Response response = restAssuredClient.executeGetRequestWithPaging(
            Optional.of(1), Optional.of(0), List.of(
                                                    "unknownSortField" + "," + SortableField.ASC
            ),
            sortEndpointDescription.getUrl(),
            getATokenWithValidCredentials().roles(List.of(RoleEnum.USER))
                .build().fetchTokenForRole()
        );

        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(),
                                       response);
    }

    protected abstract void saveToDatabase(T keyable);

    private void runTest(RestFilterEndpointDescription<T> filterDescription) throws Exception {
         runTest(filterDescription, false);
    }

    private void runTest(RestFilterEndpointDescription<T> filterDescription,
                         boolean caseInsensitive) throws Exception {
        Response response = runTest(
            filterDescription,
            req ->
                applyQueryForStart(filterDescription, req)
        );

        // assert that we have found the data with the query
        assertStart(response, filterDescription);
    }

    private void runTestPartial(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        runTestPartial(filterDescription, false);
    }

    private void runTestPartial(RestFilterEndpointDescription<T> filterDescription, boolean caseInsensitiveMatch) throws Exception {

        Response response = runTest(
            filterDescription,
            req ->
                applyQueryForStart(filterDescription, req, caseInsensitiveMatch)
        );

        assertStart(response, filterDescription);

        response = runTest(
            filterDescription,
            req ->
                applyQueryForStart(filterDescription, req, PartialEnum.START_OF_FILTER, caseInsensitiveMatch)
        );

        assertStart(response, filterDescription);

        response = runTest(
            filterDescription,
            req ->
                applyQueryForStart(filterDescription, req, PartialEnum.MIDDLE_OF_FILTER, caseInsensitiveMatch)
        );

        assertStart(response, filterDescription);

        response = runTest(
            filterDescription,
            req ->
                applyQueryForStart(filterDescription, req, PartialEnum.END_OF_FILTER, caseInsensitiveMatch)
        );

        assertStart(response, filterDescription);

    }

    private boolean isPartialOnlyConfig(RestFilterEndpointDescription<T> filterDescription) {
        for (FilterFieldData<T> data : filterDescription
            .getFilterableScenario().getStartData()) {
            if (data instanceof PartialFilterData partialFilterData == false) {
                return false;
            }
        }
        return true;
    }


    private void assertStart(Response response, RestFilterEndpointDescription<T> filterDescription) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        Assertions.assertTrue(
            assertResponseInOrder(
                List.of(filterDescription.getFilterableScenario()
                            .getStartData().getFirst().getKeyableValues().getKeyable()),
                response, true
            ));
    }

    /**
     * asserts all filter data sorted according to the default sort.
     * @param response The actual response
     * @param filterDescription The filter description.
     */
    private void assertAllFilterWithDefaultSort(Response response, RestFilterEndpointDescription<T> filterDescription) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        // gets the default sort descriptor
        SortDataDescriptor<T> sortDataDescriptor = getDefaultSort(filterDescription.getSortDescriptors());

        sortKeyables(filterDescription.getFilterableScenario().getAllKeyable(), sortDataDescriptor, null);

        Assertions.assertTrue(
            assertResponseInOrder(
                filterDescription.getFilterableScenario().getAllKeyable(),
                response, false
            ));
    }

    private Response runTest(RestFilterEndpointDescription<T> filterDescription,
                             UnaryOperator<RequestSpecification> requestSpecificationConsumer) throws Exception {
        return runTest(filterDescription, requestSpecificationConsumer, 1);
    }

    private Response runTest(RestFilterEndpointDescription<T> filterDescription,
                             UnaryOperator<RequestSpecification> requestSpecificationConsumer,
                             int pageSize) throws Exception {
        return restAssuredClient.executeGetRequestWithPaging(
            Optional.of(pageSize), Optional.of(0), new ArrayList<>(),
            filterDescription.getUrl(),
            getATokenWithValidCredentials().roles(List.of(RoleEnum.USER))
                .build().fetchTokenForRole(),
            requestSpecificationConsumer
        );
    }

    private RequestSpecification applyQueryForStart(RestFilterEndpointDescription<T> filterSortableDescription,
                                                    RequestSpecification requestSpecification) {
        return applyQueryForStart(filterSortableDescription, requestSpecification, null, false);
    }

    private RequestSpecification applyQueryForStart(RestFilterEndpointDescription<T> filterSortableDescription,
                                                    RequestSpecification requestSpecification,
                                                    boolean caseInsensitiveMatch) {
        return applyQueryForStart(filterSortableDescription, requestSpecification, null, caseInsensitiveMatch);
    }

    private RequestSpecification applyQueryForStart(RestFilterEndpointDescription<T> filterSortableDescription,
                                                    RequestSpecification requestSpecification,
                                                    PartialEnum partialEnum) {
        return applyQueryForStart(filterSortableDescription, requestSpecification, partialEnum, false);
    }


    /**
     * apply all relevant query params for the start of the scenario.
     * @param filterSortableDescription the filter and sort description.
     * @param requestSpecification      the request specification.
     */
    private RequestSpecification applyQueryForStart(RestFilterEndpointDescription<T> filterSortableDescription,
                                                    RequestSpecification requestSpecification,
                                                    PartialEnum partialEnum, boolean caseInsensitiveMatch) {
        for (FilterFieldData<T> data : filterSortableDescription
            .getFilterableScenario().getStartData()) {
            FilterValue<T> filterValue = data.getKeyableValues();

            if (data instanceof PartialFilterData partialFilterData) {
                if (partialEnum == PartialEnum.START_OF_FILTER) {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? partialFilterData.getStartsWith().toUpperCase()
                            : partialFilterData.getStartsWith());
                } else if (partialEnum == PartialEnum.MIDDLE_OF_FILTER) {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? partialFilterData.getMiddleWith().toUpperCase() :
                            partialFilterData.getMiddleWith());
                } else if (partialEnum == PartialEnum.END_OF_FILTER) {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? partialFilterData.getEndsWith().toUpperCase()
                            : partialFilterData.getEndsWith());
                } else if (partialEnum == PartialEnum.ALL_PARTIALS_IN_SCENARIO) {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? partialFilterData.getMatchOnAllPartials().toUpperCase() :
                            partialFilterData.getMatchOnAllPartials());
                } else {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? filterValue.getValue().toString().toUpperCase() : filterValue.getValue()
                    );
                }
            } else {
                requestSpecification.queryParam(
                    data.getDescriptor().getQueryName(),
                    data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                        ? filterValue.getValue().toString().toUpperCase() : filterValue.getValue()
                );
            }
        }
        return requestSpecification;
    }


    private SortDataDescriptor<T> getDefaultSort(List<SortDescriptorEnum<T>> descriptors) {
        for (SortDescriptorEnum<T> descriptor : descriptors) {
            if (descriptor.getDescriptor().isDefaultSort()) {
                return descriptor.getDescriptor();
            }
        }
        throw new FilterProcessingException("Should always have a default sort");
    }

    /**
     * Sorts the keyables based on the sort descriptor.
     * @param keysToSort the keys to sort.
     * @param sortDataDescriptor The sort descriptor to use.
     */
    private void sortKeyables(List<T> keysToSort,
                              SortDataDescriptor<T> sortDataDescriptor, String order) {
        // now run the sort based on the filter
        KeyableSortComparator comparator = new KeyableSortComparator();
        comparator.setSortField(sortDataDescriptor);

        // choose which way to sort
        if ((order == null && sortDataDescriptor.getOrder().equals(SortableField.ASC) )
            || (order.equals(SortableField.ASC))) {
            keysToSort.sort(comparator);
        } else {
            keysToSort.sort(comparator.reversed());
        }
    }

    protected abstract boolean assertResponseInOrder(List<T> keyable, Response response, boolean exactMatch);

}
