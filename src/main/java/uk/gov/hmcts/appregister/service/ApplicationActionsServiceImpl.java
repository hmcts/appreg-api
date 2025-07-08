package uk.gov.hmcts.appregister.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.model.Application;
import uk.gov.hmcts.appregister.model.ApplicationList;
import uk.gov.hmcts.appregister.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.repository.ApplicationRepository;
import uk.gov.hmcts.appregister.service.api.ApplicationActionsService;

@Service
@RequiredArgsConstructor
public class ApplicationActionsServiceImpl implements ApplicationActionsService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationListRepository applicationListRepository;

    @Override
    @Transactional
    public void moveApplications(List<Long> applicationIds, Long targetListId, String userId) {
        List<Application> applications =
                applicationRepository.findByIdInAndApplicationListUserId(applicationIds, userId);

        if (applications.size() != applicationIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "One or more applications are not accessible by this user");
        }

        // TODO: Should we all user to create a new list if the target list doesn't exist?
        ApplicationList targetList =
                applicationListRepository
                        .findByIdAndUserId(targetListId, userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Target application list not found"));

        applications.forEach(app -> app.setApplicationList(targetList));
        applicationRepository.saveAll(applications);
    }
}
