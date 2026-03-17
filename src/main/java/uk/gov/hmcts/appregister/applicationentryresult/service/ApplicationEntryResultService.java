package uk.gov.hmcts.appregister.applicationentryresult.service;

import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForCreateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadForUpdateEntryResult;
import uk.gov.hmcts.appregister.applicationentryresult.model.PayloadGetEntryResultInList;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.ApplicationListPage;
import uk.gov.hmcts.appregister.generated.model.ResultCreateDto;
import uk.gov.hmcts.appregister.generated.model.ResultGetDto;
import uk.gov.hmcts.appregister.generated.model.ResultPage;

/**
 * Service interface for managing application list entry results.
 */
public interface ApplicationEntryResultService {
    void delete(ListEntryResultDeleteArgs args);

    /**
     * Creates a new application list entry result.
     *
     * @param resultCreateDto payload containing the data required to create the entry result
     * @return a {@link MatchResponse} containing the created {@link ResultGetDto}
     */
    MatchResponse<ResultGetDto> create(
            PayloadForCreateEntryResult<ResultCreateDto> resultCreateDto);

    /**
     * Updates an application entry result.
     *
     * @param updateEntryResult The entry result update data that is representing the result data to
     *     be updated
     * @return The result get inside a match response which contains an etag
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException Data is validated
     *     for:- - The application list found and/or in the correct state - The application list
     *     entry found and/or in the correct state - The application list entry result found
     */
    MatchResponse<ResultGetDto> update(PayloadForUpdateEntryResult updateEntryResult);

    /**
     * Retrieves a paginated collection of Application List Entry Results matching the specified
     * filter criteria.
     *
     * <p>The returned {@link ApplicationListPage} includes pagination metadata (page number, total
     * elements, total pages) and a list of {@link
     * uk.gov.hmcts.appregister.generated.model.ApplicationListGetSummaryDto} items representing
     * summarized application list entries.
     *
     * @param payloadGetEntryResultInList the filter criteria
     * @param pageable pagination and sorting configuration
     * @return an {@link ResultPage} containing a paginated set of application list summaries
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if invalid filter
     *     parameters are provided or underlying data retrieval fails
     */
    ResultPage search(
            PayloadGetEntryResultInList payloadGetEntryResultInList, PagingWrapper pageable);
}
