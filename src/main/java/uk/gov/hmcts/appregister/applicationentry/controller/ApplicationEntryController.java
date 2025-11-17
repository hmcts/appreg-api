package uk.gov.hmcts.appregister.applicationentry.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.appregister.generated.api.ApplicationListEntriesApi;
import uk.gov.hmcts.appregister.generated.model.EntryGetFilterDto;
import uk.gov.hmcts.appregister.generated.model.EntryPage;

public class ApplicationEntryController implements ApplicationListEntriesApi {
    @Override
    public ResponseEntity<EntryPage> getEntries(
            EntryGetFilterDto filter, Integer page, Integer size, List<String> sort) {
        return ApplicationListEntriesApi.super.getEntries(filter, page, size, sort);
    }
}
