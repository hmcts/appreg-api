package uk.gov.hmcts.appregister.applicationentry.service;

import org.springframework.data.domain.Pageable;
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
}
