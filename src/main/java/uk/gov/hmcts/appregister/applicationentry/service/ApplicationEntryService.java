package uk.gov.hmcts.appregister.applicationentry.service;

import java.util.UUID;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.common.util.PagingWrapper;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;
import uk.gov.hmcts.appregister.generated.model.MoveEntriesDto;

public interface ApplicationEntryService {
    /**
     * Search the application entries based on the provided filter and pagination details.
     *
     * @param filterDto The filter data
     * @param pageable The pagination information
     * @return The entry page containing the search results
     */
    EntryPage search(EntryGetFilterDto filterDto, PagingWrapper pageable);

    /**
     * Creates an application entry. A fee status record(s) is created for the entry if provided,
     * officials are created if provided as well as applicant and respondants are created if
     * provided. The code works according to the rules prescribed by the defined application code.
     *
     * @param entryCreateDto The entry create dto with an id representing the list
     * @return The entry get detail inside of a match response which contains an etag
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException Data is validated
     *     for:- - The application list found and/or in the correct state - The application code is
     *     expecting a fee and it is provided - Suitable Applicants is expected - Suitable
     *     Respondent is expected ......
     */
    MatchResponse<EntryGetDetailDto> createEntry(PayloadForCreate<EntryCreateDto> entryCreateDto);

    /**
     * Updates an application entry. A fee status record(s) is created for the entry if provided,
     * officials are created if provided as well as applicant and respondents are created if
     * provided. The code works according to the rules prescribed by the defined application code.
     *
     * @param updateEntry The entry update data that representing the list data to be update
     * @return The entry get detail inside of a match response which contains an etag
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException Data is validated
     *     for:- - The application list found and/or in the correct state - The application code is
     *     expecting a fee and it is provided - Suitable Applicants is expected - Suitable
     *     Respondent is expected ......
     */
    MatchResponse<EntryGetDetailDto> updateEntry(PayloadForUpdateEntry updateEntry);

    /**
     * Retrieves an entry representation based on the entry details provided which contains the list
     * id and entry id.
     *
     * @param entry The payment get detail
     * @return A MatchResponse containing the entry details
     */
    MatchResponse<EntryGetDetailDto> getApplicationListEntryDetail(PayloadGetEntryInList entry);

    /**
     * Moves the specified entries from a source Application List to a destination Application List.
     *
     * <p>This operation transfers one or more entries currently belonging to the source list
     * identified by {@code listId} to the destination list specified within the provided {@link
     * MoveEntriesDto}.
     *
     * @param listId the identifier of the source Application List that currently owns the entries.
     * @param moveEntriesDto details of the destination list and the entries to be moved.
     * @throws uk.gov.hmcts.appregister.common.exception.AppRegistryException if validation fails,
     *     or the associated target ApplicationList entity is not found
     */
    void move(UUID listId, MoveEntriesDto moveEntriesDto);
}
