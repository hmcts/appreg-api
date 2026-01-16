package uk.gov.hmcts.appregister.applicationentry.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateEntry;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.common.model.PayloadForCreate;
import uk.gov.hmcts.appregister.generated.model.EntryCreateDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

public interface ApplicationEntryService {
    /**
     * Search the application entries based on the provided filter and pagination details.
     *
     * @param filterDto The filter data
     * @param pageable The pagination information
     * @return The entry page containing the search results
     */
    EntryPage search(EntryGetFilterDto filterDto, Pageable pageable);

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
}
