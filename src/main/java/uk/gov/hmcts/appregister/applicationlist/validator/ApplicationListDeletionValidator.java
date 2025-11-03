package uk.gov.hmcts.appregister.applicationlist.validator;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.validator.Validator;

/**
 * This class represents a validator for deleting an application list entry. It validates in two
 * ways:-
 *
 * <p>1) Ensures the id exists to be deleted 2) Ensures the id is not already soft deleted
 */
@RequiredArgsConstructor
@Component
public class ApplicationListDeletionValidator implements Validator<UUID, Void> {
    private final ApplicationListRepository applicationListRepository;

    @Override
    public void validate(UUID deletionId) {
        Optional<ApplicationList> entry = applicationListRepository.findByUuid(deletionId);

        if (entry.isEmpty()) {
            throw new AppRegistryException(
                    ApplicationListError.DELETION_ID_NOT_FOUND,
                    "Application list id %s not found".formatted(deletionId));
        }

        if (entry.get().isDeleted()) {
            throw new AppRegistryException(
                    ApplicationListError.DELETION_ALREADY_IN_DELETABLE_STATE,
                    "Application list id %s is in a deletable state".formatted(deletionId));
        }
    }
}
