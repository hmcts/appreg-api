package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
class DeleteApplicationListEntryValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private DeleteApplicationListEntryValidator deleteApplicationListEntryValidator;

    @Test
    void testSuccessfulValidation() {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));

        // validate. No errors means success
        deleteApplicationListEntryValidator.validate(payloadForDeleteEntry);
    }

    @Test
    void testFailureValidationNoListId() {
        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.empty());

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType(),
                throwable.getCode().getCode().getType());
    }

    @Test
    void testFailureValidationNoListEntryId() {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.empty());

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.LIST_ENTRY_NOT_FOUND.getCode().getType(),
                throwable.getCode().getCode().getType());
    }

    @Test
    void testFailureValidationNoAppListContainedEntryId() {
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.empty());

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType(),
                throwable.getCode().getCode().getType());
    }

    @Test
    void testFailureValidationEntryIdAlreadyDeleted() {
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);

        // The app list should not be deleted
        applicationListEntry.setDeleted(true);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode().getType(),
                throwable.getCode().getCode().getType());
    }

    @Test
    void testFailureValidationClosedApplicationList() {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.CLOSED);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType(),
                throwable.getCode().getCode().getType());
    }

    @Test
    void testFailureValidationDeletedApplicationList() {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(true);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuidIncludingDelete(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));

        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType(),
                throwable.getCode().getCode().getType());
    }
}
