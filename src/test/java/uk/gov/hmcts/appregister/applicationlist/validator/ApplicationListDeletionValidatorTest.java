package uk.gov.hmcts.appregister.applicationlist.validator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.data.AppListTestData;

@ExtendWith(MockitoExtension.class)
class ApplicationListDeletionValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @InjectMocks private ApplicationListDeletionValidator validator;

    @Test
    void validationSuccess() {
        ApplicationList applicationList = new AppListTestData().someMinimal().build();
        UUID uuid = UUID.randomUUID();
        when(applicationListRepository.findByUuid(eq(uuid)))
                .thenReturn(Optional.of(applicationList));
        validator.validate(uuid);
    }

    @Test
    void validationFailNotFound() {
        UUID uuid = UUID.randomUUID();
        when(applicationListRepository.findByUuid(eq(uuid))).thenReturn(Optional.empty());

        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(uuid));
        Assertions.assertEquals(ApplicationListError.DELETION_ID_NOT_FOUND, ex.getCode());
    }

    @Test
    void validationFailConflict() {
        ApplicationList applicationList = new AppListTestData().someMinimal().build();
        applicationList.setDeleted(true);
        UUID uuid = UUID.randomUUID();
        when(applicationListRepository.findByUuid(eq(uuid)))
                .thenReturn(Optional.of(applicationList));

        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(uuid));
        Assertions.assertEquals(
                ApplicationListError.DELETION_ALREADY_IN_DELETABLE_STATE, ex.getCode());
    }
}
