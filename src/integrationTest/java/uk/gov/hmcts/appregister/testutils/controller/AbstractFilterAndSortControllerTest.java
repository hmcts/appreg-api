package uk.gov.hmcts.appregister.testutils.controller;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;
import uk.gov.hmcts.appregister.common.mapper.SortableField;
import uk.gov.hmcts.appregister.common.security.RoleEnum;
import uk.gov.hmcts.appregister.common.serializer.StrictLocalTimeSerializer;
import uk.gov.hmcts.appregister.data.filter.FilterFieldData;
import uk.gov.hmcts.appregister.data.filter.FilterFieldValue;
import uk.gov.hmcts.appregister.data.filter.FilterableScenario;
import uk.gov.hmcts.appregister.data.filter.PartialFilterFieldData;
import uk.gov.hmcts.appregister.data.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.data.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.data.filter.sort.KeyableSortComparator;
import uk.gov.hmcts.appregister.testutils.BaseIntegration;
import uk.gov.hmcts.appregister.testutils.TransactionalUnitOfWork;
import uk.gov.hmcts.appregister.testutils.stubs.wiremock.DatabasePersistance;
import uk.gov.hmcts.appregister.testutils.token.TokenGenerator;
import uk.gov.hmcts.appregister.testutils.util.ProblemAssertUtil;

/**
 * A base class that allows for basic filter and sort testing. Each filter and sort endpoint should
 * implement this class.
 */
@Tag("FilterAndSort")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractFilterAndSortControllerTest<T extends Keyable>
        extends BaseIntegration {
    @Autowired protected DatabasePersistance persistance;

    @Autowired private EntityManager entityManager;

    public enum PartialEnum {
        START_OF_FILTER,
        MIDDLE_OF_FILTER,
        END_OF_FILTER,
        ALL_PARTIALS_IN_SCENARIO
    }

    /**
     * The stream of filter descriptions to be run.
     *
     * @return The filter endpoint descriptions.
     */
    protected abstract Stream<RestFilterEndpointDescription<T>> getFilterDescriptions()
            throws Exception;

    /**
     * The stream of sort descriptions to be run.
     *
     * @return The filter endpoint descriptions.
     */
    protected abstract Stream<RestSortEndpointDescription<T>> getSortDescriptions()
            throws Exception;

    @Autowired private TransactionalUnitOfWork transactionalUnitOfWork;

    @BeforeEach
    public void setUp() throws Exception {
        Jwt jwt = TokenGenerator.builder().build().getJwtFromToken();
        var auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runFilter(RestFilterEndpointDescription<T> filterDescription) throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        // filter using the start data of the scenario
        Response response =
                runTest(
                        filterDescription,
                        req -> applyQueryForStart(filterDescription, req, false));

        assertResponseInOrder(
                List.of(
                        filterDescription
                                .getFilterableScenario()
                                .getFilterData()
                                .getFirst()
                                .getFirst()
                                .getKeyableValues()
                                .getKeyable()),
                response);
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runFilterCaseInsensitive(RestFilterEndpointDescription<T> filterDescription)
            throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        // filter using the start data of the scenario
        Response response =
                runTest(filterDescription, req -> applyQueryForStart(filterDescription, req, true));
        assertResponseInOrder(
                List.of(filterDescription.getFilterableScenario().getAllKeyable().getFirst()),
                response);
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runPartialFilterGetAll(RestFilterEndpointDescription<T> filterDescription)
            throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        if (filterDescription.getFilterableScenario().isPartialOnlyConfig()) {
            Response response =
                    runTest(
                            filterDescription,
                            req ->
                                    applyQueryForStart(
                                            filterDescription,
                                            req,
                                            PartialEnum.ALL_PARTIALS_IN_SCENARIO),
                            100,
                            null,
                            null);

            assertAllFilterWithDefaultSort(response, filterDescription);
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runWithAllPartialCombinations(RestFilterEndpointDescription<T> filterDescription)
            throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        if (filterDescription.getFilterableScenario().doesPartialExist()) {
            runAndAssertTestPartial(filterDescription, false);
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runPartialFilterGetAllWithEachSort(
            RestFilterEndpointDescription<T> filterDescription) throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());
        Assertions.assertTrue(
                filterDescription.getFilterableScenario().getSortDescriptorEnums().size() > 1);
        if (filterDescription.getFilterableScenario().isPartialOnlyConfig()) {
            // run the sorts across each filter
            for (SortMetaDescriptorEnum<T> sort :
                    filterDescription.getFilterableScenario().getSortDescriptorEnums()) {
                Response response =
                        runTest(
                                filterDescription,
                                req ->
                                        applyQueryForStart(
                                                filterDescription,
                                                req,
                                                PartialEnum.ALL_PARTIALS_IN_SCENARIO),
                                100,
                                sort.getDescriptor().getSortableOperationEnum().getApiValue(),
                                sort.getDescriptor().getOrder());

                assertAllFilterWithSort(response, filterDescription, sort.getDescriptor());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runEachSortAscending(RestSortEndpointDescription<T> sortEndpointDescription)
            throws Exception {
        List<T> keyables = saveKeyables(sortEndpointDescription.getExpectedToBeGenerated());

        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(100),
                        Optional.of(0),
                        List.of(
                                sortEndpointDescription
                                                .getSortDescriptors()
                                                .getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue()
                                        + ","
                                        + SortableField.ASC),
                        sortEndpointDescription.getUrl(),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.USER))
                                .build()
                                .fetchTokenForRole());

        // run the assertions
        transactionalUnitOfWork.inTransaction(
                () -> {
                    List<T> reloaded = reload(keyables);
                    sortKeyables(
                            reloaded,
                            sortEndpointDescription.getSortDescriptors().getDescriptor(),
                            SortableField.ASC);
                    Assertions.assertTrue(assertResponseInOrder(reloaded, response));
                });
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("getSortDescriptions")
    public void runEachSortDescending(RestSortEndpointDescription<T> sortEndpointDescription)
            throws Exception {
        List<T> keyables = saveKeyables(sortEndpointDescription.getExpectedToBeGenerated());

        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(100),
                        Optional.of(0),
                        List.of(
                                sortEndpointDescription
                                                .getSortDescriptors()
                                                .getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue()
                                        + ","
                                        + SortableField.DESC),
                        sortEndpointDescription.getUrl(),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.USER))
                                .build()
                                .fetchTokenForRole());

        // run the assertions
        transactionalUnitOfWork.inTransaction(
                () -> {
                    List<T> reloaded = reload(keyables);
                    sortKeyables(
                            reloaded,
                            sortEndpointDescription.getSortDescriptors().getDescriptor(),
                            SortableField.DESC);
                    Assertions.assertTrue(assertResponseInOrder(reloaded, response));
                });
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("getSortDescriptions")
    public void runPageSuccess(RestSortEndpointDescription<T> sortEndpointDescription)
            throws Exception {
        List<T> keyables = saveKeyables(sortEndpointDescription.getExpectedToBeGenerated());

        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(sortEndpointDescription.getExpectedToBeGenerated().size() / 2),
                        Optional.of(1),
                        List.of(
                                sortEndpointDescription
                                                .getSortDescriptors()
                                                .getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue()
                                        + ","
                                        + SortableField.DESC),
                        sortEndpointDescription.getUrl(),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.USER))
                                .build()
                                .fetchTokenForRole());

        // run the assertions
        transactionalUnitOfWork.inTransaction(
                () -> {
                    sortKeyables(
                            reload(keyables),
                            sortEndpointDescription.getSortDescriptors().getDescriptor(),
                            SortableField.DESC);
                    List<T> selectedKeyables =
                            keyables.subList(keyables.size() / 2, keyables.size());
                    Assertions.assertTrue(assertPageSize(selectedKeyables.size(), response));
                });
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runSortFailure(RestSortEndpointDescription<T> sortEndpointDescription)
            throws Exception {
        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of(
                                sortEndpointDescription
                                                .getSortDescriptors()
                                                .getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue()
                                        + ","
                                        + SortableField.DESC,
                                sortEndpointDescription
                                                .getAvailableSortDescriptorsExcludingActive()
                                                .get(0)
                                                .getDescriptor()
                                                .getSortableOperationEnum()
                                                .getApiValue()
                                        + ","
                                        + SortableField.ASC),
                        sortEndpointDescription.getUrl(),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.USER))
                                .build()
                                .fetchTokenForRole());

        ProblemAssertUtil.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode(), response);
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runSortUnknown(RestSortEndpointDescription<T> sortEndpointDescription)
            throws Exception {
        Response response =
                restAssuredClient.executeGetRequestWithPaging(
                        Optional.of(1),
                        Optional.of(0),
                        List.of("unknownSortField" + "," + SortableField.ASC),
                        sortEndpointDescription.getUrl(),
                        getATokenWithValidCredentials()
                                .roles(List.of(RoleEnum.USER))
                                .build()
                                .fetchTokenForRole());

        ProblemAssertUtil.assertEquals(CommonAppError.SORT_NOT_SUITABLE.getCode(), response);
    }

    /**
     * save the keyables to the database.
     *
     * @param keyable The keyable to save.
     * @return The saved keyable.
     */
    protected abstract T saveToDatabase(T keyable);

    /**
     * Runs and asserts the tests for a partial filter.
     *
     * @param filterDescription The filter description.
     * @param caseInsensitiveMatch Whether to run the test with case insensitive matching.
     */
    private void runAndAssertTestPartial(
            RestFilterEndpointDescription<T> filterDescription, boolean caseInsensitiveMatch)
            throws Exception {
        Response response =
                runTest(
                        filterDescription,
                        req -> applyQueryForStart(filterDescription, req, caseInsensitiveMatch));

        assertStart(response, filterDescription);

        // test the partial data
        response =
                runTest(
                        filterDescription,
                        req ->
                                applyQueryForStart(
                                        filterDescription,
                                        req,
                                        PartialEnum.START_OF_FILTER,
                                        caseInsensitiveMatch));

        assertStart(response, filterDescription);

        // test the partial middle data filter
        response =
                runTest(
                        filterDescription,
                        req ->
                                applyQueryForStart(
                                        filterDescription,
                                        req,
                                        PartialEnum.MIDDLE_OF_FILTER,
                                        caseInsensitiveMatch));

        assertStart(response, filterDescription);

        // test the end data filter.
        response =
                runTest(
                        filterDescription,
                        req ->
                                applyQueryForStart(
                                        filterDescription,
                                        req,
                                        PartialEnum.END_OF_FILTER,
                                        caseInsensitiveMatch));

        assertStart(response, filterDescription);
    }

    private void assertStart(
            Response response, RestFilterEndpointDescription<T> filterDescription) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        transactionalUnitOfWork.inTransaction(
                () -> {
                    Assertions.assertTrue(
                            assertResponseInOrder(
                                    reload(
                                            List.of(
                                                    filterDescription
                                                            .getFilterableScenario()
                                                            .getFilterData()
                                                            .getFirst()
                                                            .getFirst()
                                                            .getKeyableValues()
                                                            .getKeyable())),
                                    response));
                });
    }

    /**
     * asserts all of the filter objects data according to the default sort.
     *
     * @param response The actual response
     * @param filterDescription The filter description.
     */
    private void assertAllFilterWithDefaultSort(
            Response response, RestFilterEndpointDescription<T> filterDescription) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        // gets the default sort descriptor
        SortMetaDataDescriptor<T> sortDataDescriptor =
                getDefaultSort(filterDescription.getSortDescriptors());

        assertAllFilterWithSort(response, filterDescription, sortDataDescriptor);
    }

    /**
     * asserts all filter data sorted according to the specific sort.
     *
     * @param response The actual response
     * @param filterDescription The filter description.
     */
    private void assertAllFilterWithSort(
            Response response,
            RestFilterEndpointDescription<T> filterDescription,
            SortMetaDataDescriptor<T> descriptor) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        transactionalUnitOfWork.inTransaction(
                () -> {
                    List<T> keyables =
                            reload(filterDescription.getFilterableScenario().getAllKeyable());

                    sortKeyables(keyables, descriptor, null);

                    Assertions.assertTrue(assertResponseInOrder(keyables, response));
                });
    }

    /**
     * runs a test with a page size of 100 records.
     *
     * @param filterDescription The filter description.
     * @param requestSpecificationConsumer The function to setup the request.
     * @return The response.
     */
    private Response runTest(
            RestFilterEndpointDescription<T> filterDescription,
            UnaryOperator<RequestSpecification> requestSpecificationConsumer)
            throws Exception {
        return runTest(filterDescription, requestSpecificationConsumer, 100, null, null);
    }

    /**
     * runs a test with a page size and a specific sort.
     *
     * @param filterDescription The filter description.
     * @param requestSpecificationConsumer The function to setup the request.
     * @param pageSize The page size.
     * @param sort The sort to use.
     * @param direction The direction to sort.
     * @return The response.
     */
    private Response runTest(
            RestFilterEndpointDescription<T> filterDescription,
            UnaryOperator<RequestSpecification> requestSpecificationConsumer,
            int pageSize,
            String sort,
            String direction)
            throws Exception {
        return restAssuredClient.executeGetRequestWithPaging(
                Optional.of(pageSize),
                Optional.of(0),
                sort != null ? List.of(sort + "," + direction) : List.of(),
                filterDescription.getUrl(),
                getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole(),
                requestSpecificationConsumer);
    }

    private RequestSpecification applyQueryForStart(
            RestFilterEndpointDescription<T> filterSortableDescription,
            RequestSpecification requestSpecification,
            boolean caseInsensitiveMatch) {
        return applyQueryForStart(
                filterSortableDescription, requestSpecification, null, caseInsensitiveMatch);
    }

    private RequestSpecification applyQueryForStart(
            RestFilterEndpointDescription<T> filterSortableDescription,
            RequestSpecification requestSpecification,
            PartialEnum partialEnum) {
        return applyQueryForStart(
                filterSortableDescription, requestSpecification, partialEnum, false);
    }

    /**
     * apply all relevant filter query params for the first record of the scenario.
     *
     * @param filterSortableDescription the filter rest description.
     * @param requestSpecification the request specification.
     * @param partialEnum The partial enum to use the start, middle or end partial filter. Null to
     *     use the full filter value.
     * @param caseInsensitiveMatch Whether to run the test with case insensitive matching.
     */
    private RequestSpecification applyQueryForStart(
            RestFilterEndpointDescription<T> filterSortableDescription,
            RequestSpecification requestSpecification,
            PartialEnum partialEnum,
            boolean caseInsensitiveMatch) {
        for (FilterFieldData<T> data :
                filterSortableDescription.getFilterableScenario().getFilterData().getFirst()) {
            FilterFieldValue<T> filterValue = data.getKeyableValues();

            if (data instanceof PartialFilterFieldData<T> partialFilterData) {
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
                                    ? partialFilterData.getMiddleWith().toUpperCase()
                                    : partialFilterData.getMiddleWith());
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
                                    ? partialFilterData.getMatchOnAllPartials().toUpperCase()
                                    : partialFilterData.getMatchOnAllPartials());
                } else {
                    requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                    ? filterValue.getValue().toString().toUpperCase()
                                    : filterValue.getValue());
                }
            } else {
                requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? filterValue.getValue().toString().toUpperCase()
                                : getFilterValueQueryValue(filterValue));
            }
        }
        return requestSpecification;
    }

    /**
     * Get the string for the filter value. We can rely on toString() for the most part but when
     * working with {@link LocalTime} we need to use a custom serializer {@link
     * StrictLocalTimeSerializer}.
     */
    private String getFilterValueQueryValue(FilterFieldValue<T> filterValue) {
        return filterValue.getValue() instanceof LocalTime
                ? StrictLocalTimeSerializer.getStringForTime((LocalTime) filterValue.getValue())
                : filterValue.getValue().toString();
    }

    /**
     * gets the default sort descriptor.
     *
     * @param descriptors The list of sort descriptors.
     * @return The default sort descriptor.
     */
    private SortMetaDataDescriptor<T> getDefaultSort(List<SortMetaDescriptorEnum<T>> descriptors) {
        for (SortMetaDescriptorEnum<T> descriptor : descriptors) {
            if (descriptor.getDescriptor().isDefaultSort()) {
                return descriptor.getDescriptor();
            }
        }

        // fail if the filter does not have a default sort.
        throw new FilterProcessingException("Should always have a default sort");
    }

    /**
     * Sorts the keyables based on the sort descriptor and the order.
     *
     * @param keysToSort the keys to sort.
     * @param sortDataDescriptor The sort descriptor to use.
     * @param order The order to sort in. Should be asc or desc
     */
    private void sortKeyables(
            List<T> keysToSort, SortMetaDataDescriptor<T> sortDataDescriptor, String order) {

        // now run the sort based on the filter
        KeyableSortComparator<T> comparator = new KeyableSortComparator<T>();

        // set the sort descriptor to sort the keyables
        comparator.setSortDescriptor(sortDataDescriptor);

        // choose which direction to sort the keyables.
        if ((order == null && sortDataDescriptor.getOrder().equals(SortableField.ASC))
                || (order.equals(SortableField.ASC))) {
            keysToSort.sort(comparator);
        } else {
            keysToSort.sort(comparator.reversed());
        }
    }

    /**
     * asserts that the response has the keyables in the order they are specified in the list.
     *
     * @param keyable The keyables to assert.
     * @param response The response to assert.
     * @return True if the response is in the correct order or false otherwise.
     */
    protected abstract boolean assertResponseInOrder(List<T> keyable, Response response);

    /**
     * assets that the response has the number of keyables specified.
     *
     * @param size The number of keyables to assert.
     * @param response The response to assert.
     * @return True if the response has the correct number of keyables or false otherwise.
     */
    protected abstract boolean assertPageSize(int size, Response response);

    /**
     * save the scenario data to the database.
     *
     * @param filterableScenario The filterable scenario keyables to save.
     */
    private void saveFilterScenarioData(FilterableScenario<T> filterableScenario) {
        for (List<FilterFieldData<T>> fieldDataList : filterableScenario.getFilterData()) {
            fieldDataList
                    .getFirst()
                    .getKeyableValues()
                    .setKeyable(
                            saveToDatabase(
                                    fieldDataList.getFirst().getKeyableValues().getKeyable()));
        }
    }

    /**
     * saves the keyables.
     *
     * @param keyables The keyables to save.
     * @return The saved keyables.
     */
    private List<T> saveKeyables(List<T> keyables) {
        ArrayList<T> savedKeyables = new ArrayList<>();
        for (T keyable : keyables) {
            savedKeyables.add(saveToDatabase(keyable));
        }
        return savedKeyables;
    }

    /**
     * reloads the keyables according to the entity manager.
     *
     * @param keyables The keyables to reload.
     * @return The reloaded keyables.
     */
    private List<T> reload(List<T> keyables) {
        ArrayList<T> savedKeyables = new ArrayList<>();
        for (T keyable : keyables) {
            T managed = (T) entityManager.find(keyable.getClass(), keyable.getId());
            savedKeyables.add(managed);
        }
        return savedKeyables;
    }
}
