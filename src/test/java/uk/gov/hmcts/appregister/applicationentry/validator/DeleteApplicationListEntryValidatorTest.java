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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForDeleteEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeleteApplicationListEntryValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private DeleteApplicationListEntryValidator deleteApplicationListEntryValidator;

    @Test
    void testSuccessfulValidation() {
        ApplicationList applicationList = new ApplicationList();
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuid(payloadForDeleteEntry.getId()))
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
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuid(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.empty());
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));

        // validate. No errors means success
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
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        ApplicationList applicationList = new ApplicationList();

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuid(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.empty());
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));

        // validate. No errors means success
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

        // The app list should not be deleted
        applicationListEntry.setDeleted(false);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuid(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.empty());

        // validate. No errors means success
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

        // The app list should not be deleted
        applicationListEntry.setDeleted(true);

        PayloadForDeleteEntry payloadForDeleteEntry =
                new PayloadForDeleteEntry(UUID.randomUUID(), UUID.randomUUID());
        when(applicationListRepository.findByUuid(payloadForDeleteEntry.getId()))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidIncludingDelete(
                        payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(
                        payloadForDeleteEntry.getId(), payloadForDeleteEntry.getEntryId()))
                .thenReturn(Optional.of(applicationListEntry));

        // validate. No errors means success
        AppRegistryException throwable =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> deleteApplicationListEntryValidator.validate(payloadForDeleteEntry));
        Assertions.assertEquals(
                AppListEntryError.DELETION_ALREADY_IN_DELETABLE_STATE.getCode().getType(),
                throwable.getCode().getCode().getType());
    }
}
