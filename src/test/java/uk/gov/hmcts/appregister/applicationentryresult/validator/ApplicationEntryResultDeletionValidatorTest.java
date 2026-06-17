package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ResolutionCodeRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;

@ExtendWith(MockitoExtension.class)
class ApplicationEntryResultDeletionValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResultRepository;
    @Mock private ResolutionCodeRepository resolutionCodeRepository;
    @Mock private BusinessDateProvider businessDateProvider;

    @InjectMocks private ApplicationEntryResultDeletionValidator validator;

    @Test
    void validationSuccess() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        ApplicationListEntry entry = new ApplicationListEntry();
        AppListEntryResolution entryResult = new AppListEntryResolution();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.of(entry));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(entryId, listId))
                .thenReturn(Optional.of(entry));
        when(appListEntryResultRepository.findByUuid(resultId))
                .thenReturn(Optional.of(entryResult));
        when(appListEntryResultRepository.findByUuidAndApplicationList_Uuid(resultId, entryId))
                .thenReturn(Optional.of(entryResult));

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        validator.validate(args);
    }

    @Test
    void validationFailListNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST, ex.getCode());
    }

    @Test
    void validationFailListNotOpen() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(false);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_LIST_STATE_IS_INCORRECT, ex.getCode());
    }

    @Test
    void validationFailEntryNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_DOES_NOT_EXIST, ex.getCode());
    }

    @Test
    void validationFailEntryNotWithinList() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId))
                .thenReturn(Optional.of(new ApplicationListEntry()));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(entryId, listId))
                .thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_NOT_WITHIN_LIST, ex.getCode());
    }

    @Test
    void validationFailEntryResultNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        ApplicationListEntry entry = new ApplicationListEntry();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.of(entry));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(entryId, listId))
                .thenReturn(Optional.of(entry));
        when(appListEntryResultRepository.findByUuid(resultId)).thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST,
                ex.getCode());
    }

    @Test
    void validationFailEntryResultNotWithinEntry() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        ApplicationListEntry entry = new ApplicationListEntry();
        AppListEntryResolution entryResult = new AppListEntryResolution();

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId)).thenReturn(Optional.of(entry));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(entryId, listId))
                .thenReturn(Optional.of(entry));
        when(appListEntryResultRepository.findByUuid(resultId))
                .thenReturn(Optional.of(entryResult));
        when(appListEntryResultRepository.findByUuidAndApplicationList_Uuid(resultId, entryId))
                .thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_NOT_WITHIN_ENTRY,
                ex.getCode());
    }
}
