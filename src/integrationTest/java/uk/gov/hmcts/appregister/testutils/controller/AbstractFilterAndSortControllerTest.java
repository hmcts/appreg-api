package uk.gov.hmcts.appregister.testutils.controller;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;

import java.net.URL;
import java.security.Key;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

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
import uk.gov.hmcts.appregister.filter.FilterFieldData;
import uk.gov.hmcts.appregister.filter.FilterFieldValue;
import uk.gov.hmcts.appregister.filter.FilterableScenario;
import uk.gov.hmcts.appregister.filter.PartialFilterFieldData;
import uk.gov.hmcts.appregister.filter.exception.FilterProcessingException;
import uk.gov.hmcts.appregister.filter.meta.SortMetaDataDescriptor;
import uk.gov.hmcts.appregister.filter.meta.SortMetaDescriptorEnum;
import uk.gov.hmcts.appregister.filter.sort.KeyableSortComparator;
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
@Slf4j
public abstract class AbstractFilterAndSortControllerTest<T extends Keyable>
    extends BaseIntegration {
    @Autowired
    protected DatabasePersistance persistance;

    @Autowired
    private EntityManager entityManager;

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

    @Autowired
    protected TransactionalUnitOfWork transactionalUnitOfWork;

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

        if (isApplicableForFilterAccordingToAvailableValues(filterDescription
                                                                .getFilterableScenario().getFilterData().getFirst())) {

        // filter using the start data of the scenario
        Response response =
            runTest(
                filterDescription,
                req -> applyQueryForStart(filterDescription, req, false)
            );

        // run the assertions
        transactionalUnitOfWork.inTransaction(
            () -> {
                List<T> reloaded =
                    reload(
                        List.of(
                            filterDescription
                                .getFilterableScenario()
                                .getFilterData()
                                .getFirst()
                                .getFirst()
                                .getKeyableValues()
                                .getKeyable()));

                assertResponseInOrder(reloaded, response, List.of());
            });
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runFilterCaseInsensitive(RestFilterEndpointDescription<T> filterDescription)
        throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        // only react to filters that are applicable to case insensitive matching
        if (doesContainACaseInsensitiveFilter(filterDescription)
        && isApplicableForFilterAccordingToAvailableValues(filterDescription
                                                               .getFilterableScenario().getFilterData().getFirst())) {

            // filter using the start data of the scenario
            Response response =
                runTest(
                    filterDescription,
                    req -> applyQueryForStart(filterDescription, req, true)
                );

            transactionalUnitOfWork.inTransaction(
                () -> {
                    List<T> reloaded =
                        reload(
                            List.of(
                                filterDescription
                                    .getFilterableScenario()
                                    .getFilterData()
                                    .getFirst()
                                    .getFirst()
                                    .getKeyableValues()
                                    .getKeyable()));
                    assertResponseInOrder(
                        reloaded,
                        response,
                        getListExcludingStart(
                            filterDescription.getFilterableScenario().getAllKeyable())
                    );
                });
        }
    }

    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runPartialFilterGetAll(RestFilterEndpointDescription<T> filterDescription)
        throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        if (filterDescription.getFilterableScenario().isPartialOnlyConfig()
            && isApplicableForFilterAccordingToAvailableValues(
                filterDescription.getFilterableScenario().getFilterData().getFirst())) {
            Response response =
                runTest(
                    filterDescription,
                    req ->
                        applyQueryForStart(
                            filterDescription,
                            req,
                            PartialEnum.ALL_PARTIALS_IN_SCENARIO
                        ),
                    100,
                    null,
                    null
                );

            List<T> assertedMatchingKeys =
                getAllKeyableThatShouldPartialMatch(
                    filterDescription.getFilterableScenario(),
                    filterDescription
                        .getFilterableScenario().getFilterData().getFirst()
                );


            assertAllFilterWithDefaultSort(assertedMatchingKeys, response, filterDescription);
        }
    }


    @ParameterizedTest
    @MethodSource("getFilterDescriptions")
    public void runWithAllPartialCombinations(RestFilterEndpointDescription<T> filterDescription)
        throws Exception {
        // save all keyable data that belongs to scenario
        saveFilterScenarioData(filterDescription.getFilterableScenario());

        if (filterDescription.getFilterableScenario().doesPartialExist()
            && isApplicableForFilterAccordingToAvailableValues(filterDescription
                                                                   .getFilterableScenario()
                                                                   .getFilterData().getFirst())) {
            runAndAssertTestPartial(filterDescription, false);
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
                getsSortUrl(sortEndpointDescription),
                getATokenWithValidCredentials()
                    .roles(List.of(RoleEnum.USER))
                    .build()
                    .fetchTokenForRole()
            );

        // run the assertions
        transactionalUnitOfWork.inTransaction(
            () -> {
                List<T> reloaded = reload(keyables);
                sortKeyables(
                    reloaded,
                    sortEndpointDescription.getSortDescriptors().getDescriptor(),
                    SortableField.ASC
                );
                Assertions.assertTrue(assertResponseInOrder(reloaded, response, List.of()));
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
                getsSortUrl(sortEndpointDescription),
                getATokenWithValidCredentials()
                    .roles(List.of(RoleEnum.USER))
                    .build()
                    .fetchTokenForRole()
            );

        // run the assertions
        transactionalUnitOfWork.inTransaction(
            () -> {
                List<T> reloaded = reload(keyables);
                sortKeyables(
                    reloaded,
                    sortEndpointDescription.getSortDescriptors().getDescriptor(),
                    SortableField.DESC
                );
                Assertions.assertTrue(assertResponseInOrder(reloaded, response, List.of()));
            });
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("getSortDescriptions")
    public void runPageSuccess(RestSortEndpointDescription<T> sortEndpointDescription)
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
                getsSortUrl(sortEndpointDescription),
                getATokenWithValidCredentials()
                    .roles(List.of(RoleEnum.USER))
                    .build()
                    .fetchTokenForRole()
            );

        // run the assertions
        transactionalUnitOfWork.inTransaction(
            () -> {
                List<T> reloaded = reload(keyables);
                sortKeyables(
                    reloaded,
                    sortEndpointDescription.getSortDescriptors().getDescriptor(),
                    SortableField.ASC
                );
                Assertions.assertTrue(assertResponseInOrder(reloaded, response, List.of()));
            });
    }

    public URL getsSortUrl(RestSortEndpointDescription<T> sortEndpointDescription) {
        return transactionalUnitOfWork.inTransactionWithExceptionAndReturn(
            () -> {
                List<T> keyables = reload(sortEndpointDescription.getExpectedToBeGenerated());
                return sortEndpointDescription.getGetUrlFunction().getUrl(keyables.getFirst());
            });
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runMultiSortFailure(RestSortEndpointDescription<T> sortEndpointDescription)
        throws Exception {
        saveKeyables(sortEndpointDescription.getExpectedToBeGenerated());

        if (sortEndpointDescription.getAllAvailableSortDescriptors().size() > 1) {
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
                            + SortableField.ASC
                    ),
                    getsSortUrl(sortEndpointDescription),
                    getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole()
                );

            ProblemAssertUtil.assertEquals(
                CommonAppError.MULTIPLE_SORT_NOT_SUPPORTED.getCode(), response);
        }
    }

    @ParameterizedTest
    @MethodSource("getSortDescriptions")
    public void runSortUnknown(RestSortEndpointDescription<T> sortEndpointDescription)
        throws Exception {
        saveKeyables(sortEndpointDescription.getExpectedToBeGenerated());

        Response response =
            restAssuredClient.executeGetRequestWithPaging(
                Optional.of(1),
                Optional.of(0),
                List.of("unknownSortField" + "," + SortableField.ASC),
                getsSortUrl(sortEndpointDescription),
                getATokenWithValidCredentials()
                    .roles(List.of(RoleEnum.USER))
                    .build()
                    .fetchTokenForRole()
            );

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
     * @param filterDescription    The filter description.
     * @param caseInsensitiveMatch Whether to run the test with case insensitive matching.
     */
    private void runAndAssertTestPartial(
        RestFilterEndpointDescription<T> filterDescription, boolean caseInsensitiveMatch)
        throws Exception {
        Response response =
            runTest(
                filterDescription,
                req -> applyQueryForStart(filterDescription, req, caseInsensitiveMatch)
            );

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
                        caseInsensitiveMatch
                    )
            );

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
                        caseInsensitiveMatch
                    )
            );

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
                        caseInsensitiveMatch
                    )
            );

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
                        response,
                        getListExcludingStart(
                            filterDescription
                                .getFilterableScenario()
                                .getAllKeyable())
                    ));
            });
    }

    /**
     * Excludes the start of the list.
     *
     * @param keyables The list of keyables to exclude the start of.
     * @return The list of keyables excluding the start.
     */
    private List<T> getListExcludingStart(List<T> keyables) {
        List<T> keyablesExcludingStart = new ArrayList<>(keyables);
        keyablesExcludingStart.remove(0);
        return keyablesExcludingStart;
    }

    /**
     * asserts all of the filter objects data according to the default sort.
     *
     * @param response          The actual response
     * @param filterDescription The filter description.
     */
    private void assertAllFilterWithDefaultSort(
        List<T> assertedMatchingKeys, Response response,
        RestFilterEndpointDescription<T> filterDescription) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        // gets the default sort descriptor
        SortMetaDataDescriptor<T> sortDataDescriptor =
            getDefaultSort(filterDescription.getSortDescriptors());

        assertAllFilterWithSort(
            assertedMatchingKeys, response,
            filterDescription, sortDataDescriptor
        );
    }

    /**
     * asserts all filter data sorted according to the specific sort.
     *
     * @param response          The actual response
     * @param filterDescription The filter description.
     */
    private void assertAllFilterWithSort(
        List<T> assertedMatchingKeys,
        Response response,
        RestFilterEndpointDescription<T> filterDescription,
        SortMetaDataDescriptor<T> descriptor) {
        // assert that we have found the data with the query
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        transactionalUnitOfWork.inTransaction(
            () -> {
                List<T> keyables =
                    reload(assertedMatchingKeys);

                sortKeyables(keyables, descriptor, null);

                Assertions.assertTrue(assertResponseInOrder(keyables, response, List.of()));
            });
    }

    /**
     * gets all matching keyables in this scenario when we exclusively partially filtering.
     * This method considers that not all keyable fields for each row of the scenario
     * may be returned based on a null value being returned.
     *
     * @param scenario         The filter scenario that contains only partials.
     * @param filterProcessing The filter that we are processing.
     */
    private List<T> getAllKeyableThatShouldPartialMatch(FilterableScenario<T> scenario, List<FilterFieldData<T>> filterProcessing) {
        ArrayList<T> results = new ArrayList<>();
        for (List<FilterFieldData<T>> filterFieldDataRow : scenario.getFilterData()) {
            boolean matchingPartial = true;

            for (FilterFieldData<T> fieldOfRow : filterFieldDataRow) {
                // loop through each field for the row we are targetting
                for (FilterFieldData<T> filterFieldData : filterProcessing) {
                    // if any of the partial fields matches a row value with null
                    // then this partial field does not apply and would not match
                    // so exclude it
                    if (fieldOfRow.getDescriptor().getQueryName()
                        .equals(filterFieldData.getDescriptor().getQueryName())) {
                        if (fieldOfRow.getKeyableValues().getValue() == null) {
                            matchingPartial = false;
                        }
                    }
                }
            }

            // add the keyable to match on the partial filter output
            if (matchingPartial) {
                // if we have already captured the keyable reference they do not readd
                if (!results.contains(filterFieldDataRow.getFirst().getKeyableValues().getKeyable())) {
                    results.add(filterFieldDataRow.getFirst().getKeyableValues().getKeyable());
                }
            }
        }

        return results;
    }

    /**
     * runs a test with a page size of 100 records.
     *
     * @param filterDescription            The filter description.
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
     * @param filterDescription            The filter description.
     * @param requestSpecificationConsumer The function to setup the request.
     * @param pageSize                     The page size.
     * @param sort                         The sort to use.
     * @param direction                    The direction to sort.
     * @return The response.
     */
    private Response runTest(
        RestFilterEndpointDescription<T> filterDescription,
        UnaryOperator<RequestSpecification> requestSpecificationConsumer,
        int pageSize,
        String sort,
        String direction)
        throws Exception {
        return transactionalUnitOfWork.inTransactionWithExceptionAndReturn(
            () -> {
                List<T> keyables =
                    reload(filterDescription.getFilterableScenario().getAllKeyable());

                URL urlToCall =
                    filterDescription.getGetUrlFunction().getUrl(keyables.getFirst());

                return restAssuredClient.executeGetRequestWithPaging(
                    Optional.of(pageSize),
                    Optional.of(0),
                    sort != null ? List.of(sort + "," + direction) : List.of(),
                    urlToCall,
                    getATokenWithValidCredentials()
                        .roles(List.of(RoleEnum.USER))
                        .build()
                        .fetchTokenForRole(),
                    requestSpecificationConsumer
                );
            });
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
     * @param requestSpecification      the request specification.
     * @param partialEnum               The partial enum to use the start, middle or end partial filter. Null to
     *                                  use the full filter value.
     * @param caseInsensitiveMatch      Whether to run the test with case insensitive matching.
     */
    private RequestSpecification applyQueryForStart(
        RestFilterEndpointDescription<T> filterSortableDescription,
        RequestSpecification requestSpecification,
        PartialEnum partialEnum,
        boolean caseInsensitiveMatch) {
        for (FilterFieldData<T> data :
            filterSortableDescription.getFilterableScenario().getFilterData().getFirst()) {
            FilterFieldValue<T> filterValue = data.getKeyableValues();
            if (filterValue.getValue() != null) {
                if (data instanceof PartialFilterFieldData<T> partialFilterData) {
                    if (partialEnum == PartialEnum.START_OF_FILTER) {
                        requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? partialFilterData.getStartsWith().toUpperCase()
                                : partialFilterData.getStartsWith()
                        );
                    } else if (partialEnum == PartialEnum.MIDDLE_OF_FILTER) {
                        requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? partialFilterData.getMiddleWith().toUpperCase()
                                : partialFilterData.getMiddleWith()
                        );
                    } else if (partialEnum == PartialEnum.END_OF_FILTER) {
                        requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? partialFilterData.getEndsWith().toUpperCase()
                                : partialFilterData.getEndsWith()
                        );
                    } else if (partialEnum == PartialEnum.ALL_PARTIALS_IN_SCENARIO) {
                        requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? partialFilterData.getMatchOnAllPartials().toUpperCase()
                                : partialFilterData.getMatchOnAllPartials()
                        );
                    } else {
                        requestSpecification.queryParam(
                            data.getDescriptor().getQueryName(),
                            data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                                ? filterValue.getValue().toString().toUpperCase()
                                : filterValue.getValue()
                        );
                    }
                }
            } else {
                if (filterValue.getValue() != null) {
                    requestSpecification.queryParam(
                        data.getDescriptor().getQueryName(),
                        data.getDescriptor().isCaseInsensitive() && caseInsensitiveMatch
                            ? filterValue.getValue().toString().toUpperCase()
                            : getFilterValueQueryValue(filterValue)
                    );
                }
            }
        }
        return requestSpecification;
    }

    /**
     * Make sure that the filter is applicable for the filter scenario.
     */
    private boolean isApplicableForFilterAccordingToAvailableValues(List<FilterFieldData<T>> data) {
        for (FilterFieldData<T> filterFieldData : data) {
            if (filterFieldData.getKeyableValues().getValue() != null)
                return true;
        }

        return false;
    }

    /**
     * Does this descriptor contain filter for a case insensitive match.
     *
     * @return True or false
     */
    private boolean doesContainACaseInsensitiveFilter(
        RestFilterEndpointDescription<T> filterSortableDescription) {
        for (FilterFieldData<T> data :
            filterSortableDescription.getFilterableScenario().getFilterData().getFirst()) {

            // check if case insensitive is set and the value is a string.
            if (data.getDescriptor().isCaseInsensitive()
                && data.getKeyableValues().getValue() instanceof String) {
                return true;
            }
        }

        return false;
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
     * @param keysToSort         the keys to sort.
     * @param sortDataDescriptor The sort descriptor to use.
     * @param order              The order to sort in. Should be asc or desc
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
            comparator.setDescending(false);
            keysToSort.sort(comparator);
        } else {
            comparator.setDescending(true);
            keysToSort.sort(comparator);
        }
    }

    /**
     * asserts that the response has the keyables in the order they are specified in the list.
     *
     * @param keyable   The keyables to assert exist.
     * @param response  The response to assert.
     * @param notExists The keyables that should not exist in the response.
     * @return True if the response is in the correct order or false otherwise.
     */
    protected abstract boolean assertResponseInOrder(
        List<T> keyable, Response response, List<T> notExists);

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
