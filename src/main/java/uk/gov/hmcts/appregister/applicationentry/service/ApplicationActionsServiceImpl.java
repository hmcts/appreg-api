package uk.gov.hmcts.appregister.applicationentry.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.security.UserProvider;

/** Service for handling application actions such as moving applications between lists. */
@Service
@RequiredArgsConstructor
public class ApplicationActionsServiceImpl implements ApplicationActionsService {

    private final ApplicationListEntryRepository applicationListEntryRepository;
    private final ApplicationListRepository applicationListRepository;
    private final UserProvider userProvider;

    @Override
    @Transactional
    public void moveApplications(List<Long> applicationIds, Long targetListId) {
        List<ApplicationListEntry> applications =
                applicationListEntryRepository.findByIdInAndCreatedUser(
                        applicationIds, userProvider.getUser());

        if (applications.size() != applicationIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "One or more applications are not accessible by this user");
        }

        // TODO: Should we all user to create a new list if the target list doesn't exist?
        ApplicationList targetList =
                applicationListRepository
                        .findByIdAndCreatedUser(targetListId, userProvider.getUser())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Target application list not found"));

        applications.forEach(app -> app.setApplicationList(targetList));
        applicationListEntryRepository.saveAll(applications);
    }
}
