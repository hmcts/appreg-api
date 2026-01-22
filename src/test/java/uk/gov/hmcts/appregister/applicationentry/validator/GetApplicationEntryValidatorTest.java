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
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetApplicationEntryValidatorTest {
    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private GetApplicationEntryValidator getEntryValidator;

    @Test
    void successfulValidation() {
        UUID listId = UUID.randomUUID();

        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        ApplicationListEntry applicationListEntry = new ApplicationListEntry();

        // mock core database interaction for success
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(listId, entryId))
                .thenReturn(Optional.of(applicationListEntry));

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        // run test. no exception is a sign of success
        getEntryValidator.validate(payloadGetEntryInList);
    }

    @Test
    void invalidListUuid() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        // mock that no list value was found
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        // run validation
        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> getEntryValidator.validate(payloadGetEntryInList));

        // assert exception
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST.getCode().getType(),
                exception.getCode().getCode().getType());
    }

    @Test
    void invalidListStateClosedUuid() {
        UUID listId = UUID.randomUUID();

        // mock that no list value was found with the correct state
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.CLOSED);
        applicationList.setDeleted(YesOrNo.NO);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));

        // run validation
        UUID entryId = UUID.randomUUID();

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> getEntryValidator.validate(payloadGetEntryInList));

        // assert exception
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType(),
                exception.getCode().getCode().getType());
    }

    @Test
    void invalidListStateDeletedUuid() {
        UUID listId = UUID.randomUUID();

        // mock that no list value was found with the correct state
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.YES);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));

        // run validation
        UUID entryId = UUID.randomUUID();

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> getEntryValidator.validate(payloadGetEntryInList));

        // assert exception
        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT.getCode().getType(),
                exception.getCode().getCode().getType());
    }

    @Test
    void invalidEntryUuid() {
        UUID listId = UUID.randomUUID();

        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        // mock that on database interaction, no list entry value was found with the correct state
        UUID entryId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.empty());

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> getEntryValidator.validate(payloadGetEntryInList));
        Assertions.assertEquals(
                AppListEntryError.ENTRY_DOES_NOT_EXIST.getCode().getType(),
                exception.getCode().getCode().getType());
    }

    @Test
    void invalidEntryNotInList() {
        UUID listId = UUID.randomUUID();

        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(Status.OPEN);
        applicationList.setDeleted(YesOrNo.NO);

        // mock that the entry is not found in the list
        ApplicationListEntry applicationListEntry = new ApplicationListEntry();
        UUID entryId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(listId, entryId))
                .thenReturn(Optional.empty());

        PayloadGetEntryInList payloadGetEntryInList =
                PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> getEntryValidator.validate(payloadGetEntryInList));
        Assertions.assertEquals(
                AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST.getCode().getType(),
                exception.getCode().getCode().getType());
    }
}
