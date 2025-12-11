package uk.gov.hmcts.appregister.applicationentry.service;

import org.springframework.data.domain.Pageable;

import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.concurrency.MatchResponse;
import uk.gov.hmcts.appregister.generated.model.EntryGetDetailDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryGetSummaryDto;
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
     * Retrieves an entry representation based on the entry details provided
     *
     * @param entry The payment get detail
     * @return A MatchResponse containing the summary of application list entries
     */
    MatchResponse<EntryGetDetailDto> getApplicationListEntrySummary(PayloadGetEntryInList entry);
}
