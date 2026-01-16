package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationentryresult.model.ListEntryResultDeleteArgs;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

@ExtendWith(MockitoExtension.class)
class ApplicationEntryResultDeletionValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResultRepository;

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

        when(applicationListRepository.findByUuid(eq(listId)))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(entryId), eq(listId)))
                .thenReturn(Optional.of(entry));
        when(appListEntryResultRepository.findByUuidAndApplicationList_Uuid(
                        eq(resultId), eq(entryId)))
                .thenReturn(Optional.of(entryResult));

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        validator.validate(args);
    }

    @Test
    void validationFailListNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        when(applicationListRepository.findByUuid(eq(listId))).thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.ENTRY_RESULT_LIST_NOT_FOUND, ex.getCode());
    }

    @Test
    void validationFailListNotOpen() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(false);
        when(applicationList.getUuid()).thenReturn(listId);

        when(applicationListRepository.findByUuid(eq(listId)))
                .thenReturn(Optional.of(applicationList));

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.INVALID_ENTRY_RESULT_LIST_STATUS, ex.getCode());
    }

    @Test
    void validationFailEntryNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        when(applicationListRepository.findByUuid(eq(listId)))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(entryId), eq(listId)))
                .thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(AppListEntryError.LIST_ENTRY_NOT_FOUND, ex.getCode());
    }

    @Test
    void validationFailEntryResultNotFound() {
        UUID listId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        ApplicationList applicationList = mock(ApplicationList.class);
        when(applicationList.isOpen()).thenReturn(true);

        ApplicationListEntry entry = new ApplicationListEntry();

        when(applicationListRepository.findByUuid(eq(listId)))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findActiveByUuidAndApplicationListUuid(
                        eq(entryId), eq(listId)))
                .thenReturn(Optional.of(entry));
        when(appListEntryResultRepository.findByUuidAndApplicationList_Uuid(
                        eq(resultId), eq(entryId)))
                .thenReturn(Optional.empty());

        ListEntryResultDeleteArgs args = new ListEntryResultDeleteArgs(listId, entryId, resultId);
        AppRegistryException ex =
                Assertions.assertThrows(AppRegistryException.class, () -> validator.validate(args));
        Assertions.assertEquals(
                ApplicationListEntryResultError.LIST_ENTRY_RESULT_NOT_FOUND, ex.getCode());
    }
}
