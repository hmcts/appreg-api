package uk.gov.hmcts.appregister.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.appregister.model.Application;
import uk.gov.hmcts.appregister.repository.ApplicationRepository;

@Service
@RequiredArgsConstructor
public class ApplicationSaveService {

    private final ApplicationRepository repository;

    @Transactional
    public void saveApplication(Application app) {
        repository.save(app);
    }
}
