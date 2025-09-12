package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;

@Service
@RequiredArgsConstructor
public class ApplicationListEntrySaveService {

    private final ApplicationListEntryRepository repository;

    @Transactional
    public void saveApplication(ApplicationListEntry app) {
        repository.save(app);
    }
}
