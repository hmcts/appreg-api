package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.applicationentry.model.Application;
import uk.gov.hmcts.appregister.applicationentry.repository.ApplicationRepository;

@Service
@RequiredArgsConstructor
public class ApplicationSaveService {

    private final ApplicationRepository repository;

    @Transactional
    public void saveApplication(Application app) {
        repository.save(app);
    }
}
