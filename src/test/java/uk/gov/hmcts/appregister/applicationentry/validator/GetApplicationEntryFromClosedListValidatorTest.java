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
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadGetEntryInList;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;

@ExtendWith(MockitoExtension.class)
class GetApplicationEntryFromClosedListValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private GetApplicationEntryFromClosedListValidator validator;

    @Test
    void givenClosedListAndMatchingEntry_whenValidate_thenReturnsSuccess() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        ApplicationList applicationList = applicationList(Status.CLOSED, YesOrNo.NO);
        ApplicationListEntry entry = new ApplicationListEntry();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.of(entry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(listId, entryId))
                .thenReturn(Optional.of(entry));

        GetEntryValidationSuccess success =
                validator.validate(
                        payload(listId, entryId),
                        (request, validationSuccess) -> validationSuccess);

        Assertions.assertSame(applicationList, success.getApplicationList());
        Assertions.assertSame(entry, success.getApplicationListEntry());
    }

    @Test
    void givenMissingList_whenValidate_thenThrowsListNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload(listId, entryId)));

        assertError(ApplicationListError.LIST_NOT_FOUND, HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void givenDeletedList_whenValidate_thenThrowsListNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList(Status.CLOSED, YesOrNo.YES)));

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload(listId, entryId)));

        assertError(ApplicationListError.LIST_NOT_FOUND, HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void givenOpenList_whenValidate_thenThrowsIncorrectState() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList(Status.OPEN, YesOrNo.NO)));

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload(listId, entryId)));

        assertError(
                AppListEntryError.APPLICATION_LIST_MUST_BE_CLOSED, HttpStatus.CONFLICT, exception);
    }

    @Test
    void givenMissingEntry_whenValidate_thenThrowsEntryNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList(Status.CLOSED, YesOrNo.NO)));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.empty());

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload(listId, entryId)));

        assertError(AppListEntryError.LIST_ENTRY_NOT_FOUND, HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void givenEntryInDifferentList_whenValidate_thenThrowsEntryNotWithinList() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        ApplicationListEntry entry = new ApplicationListEntry();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList(Status.CLOSED, YesOrNo.NO)));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.of(entry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(listId, entryId))
                .thenReturn(Optional.empty());

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(payload(listId, entryId)));

        assertError(AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST, HttpStatus.CONFLICT, exception);
    }

    private static ApplicationList applicationList(Status status, YesOrNo deleted) {
        ApplicationList applicationList = new ApplicationList();
        applicationList.setStatus(status);
        applicationList.setDeleted(deleted);
        return applicationList;
    }

    private static PayloadGetEntryInList payload(UUID listId, UUID entryId) {
        return PayloadGetEntryInList.builder().listId(listId).entryId(entryId).build();
    }

    private static void assertError(
            ErrorCodeEnum expected, HttpStatus expectedStatus, AppRegistryException exception) {
        Assertions.assertSame(expected, exception.getCode());
        Assertions.assertEquals(expectedStatus, exception.getCode().getCode().getHttpCode());
    }
}
