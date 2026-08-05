package uk.gov.hmcts.appregister.applicationentryresult.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.appregister.applicationentryresult.exception.ApplicationListEntryResultError;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.repository.AppListEntryResolutionRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultItemDto;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultsDto;

@ExtendWith(MockitoExtension.class)
class BulkApplicationEntryResultDeletionValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;
    @Mock private AppListEntryResolutionRepository appListEntryResolutionRepository;

    @InjectMocks private BulkApplicationEntryResultDeletionValidator validator;

    @Test
    void validate_whenResultsMissing_thenThrows() {
        var exception =
                assertThrows(
                        AppRegistryException.class,
                        () -> validator.validate(new BulkDeleteResultsDto()));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_PROVIDED);
    }

    @Test
    void validate_whenDuplicateItems_thenThrows() {
        var item =
                new BulkDeleteResultItemDto()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .resultId(UUID.randomUUID());
        var request = new BulkDeleteResultsDto().results(List.of(item, item));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE);
    }

    @Test
    void validate_whenListMissing_thenThrows() {
        var item =
                new BulkDeleteResultItemDto()
                        .listId(UUID.randomUUID())
                        .entryId(UUID.randomUUID())
                        .resultId(UUID.randomUUID());
        var request = new BulkDeleteResultsDto().results(List.of(item));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode())
                .isEqualTo(ApplicationListEntryResultError.APPLICATION_LIST_DOES_NOT_EXIST);
    }

    @Test
    void validate_whenValid_thenReturnsValidatedItems() {
        final UUID listId = UUID.randomUUID();
        final UUID entryId = UUID.randomUUID();
        final UUID resultId = UUID.randomUUID();

        var list = new ApplicationList();
        list.setUuid(listId);
        list.setDeleted(YesOrNo.NO);
        list.setStatus(uk.gov.hmcts.appregister.common.enumeration.Status.OPEN);

        var entry = new ApplicationListEntry();
        entry.setUuid(entryId);
        entry.setApplicationList(list);

        var code = new ResolutionCode();
        code.setResultCode("RTC");

        var result = new AppListEntryResolution();
        result.setUuid(resultId);
        result.setApplicationList(entry);
        result.setResolutionCode(code);

        when(applicationListRepository.findByUuidIncludingDeleteIn(List.of(listId)))
                .thenReturn(List.of(list));
        when(applicationListEntryRepository.findByUuidIncludingDeleteIn(List.of(entryId)))
                .thenReturn(List.of(entry));
        when(applicationListEntryRepository.findActiveByUuidsAndApplicationListUuids(
                        List.of(entryId), List.of(listId)))
                .thenReturn(List.of(entry));
        when(appListEntryResolutionRepository.findByUuidIn(List.of(resultId)))
                .thenReturn(List.of(result));

        var item = new BulkDeleteResultItemDto().listId(listId).entryId(entryId).resultId(resultId);
        var success =
                validator.validate(
                        new BulkDeleteResultsDto().results(List.of(item)),
                        (request, validated) -> validated);

        assertThat(success.getResults()).hasSize(1);
        assertThat(success.getResults().getFirst().args().listId()).isEqualTo(listId);
    }

    @Test
    void validate_whenResultNotInEntry_thenThrows() {
        final UUID listId = UUID.randomUUID();
        final UUID entryId = UUID.randomUUID();
        final UUID resultId = UUID.randomUUID();

        var list = new ApplicationList();
        list.setUuid(listId);
        list.setDeleted(YesOrNo.NO);
        list.setStatus(uk.gov.hmcts.appregister.common.enumeration.Status.OPEN);

        var activeEntry = new ApplicationListEntry();
        activeEntry.setUuid(entryId);
        activeEntry.setApplicationList(list);

        final UUID otherEntryId = UUID.randomUUID();
        var otherEntry = new ApplicationListEntry();
        otherEntry.setUuid(otherEntryId);
        otherEntry.setApplicationList(list);

        var result = new AppListEntryResolution();
        result.setUuid(resultId);
        result.setApplicationList(otherEntry);

        when(applicationListRepository.findByUuidIncludingDeleteIn(List.of(listId)))
                .thenReturn(List.of(list));
        when(applicationListEntryRepository.findByUuidIncludingDeleteIn(List.of(entryId)))
                .thenReturn(List.of(activeEntry));
        when(applicationListEntryRepository.findActiveByUuidsAndApplicationListUuids(
                        List.of(entryId), List.of(listId)))
                .thenReturn(List.of(activeEntry));
        when(appListEntryResolutionRepository.findByUuidIn(List.of(resultId)))
                .thenReturn(List.of(result));

        var item = new BulkDeleteResultItemDto().listId(listId).entryId(entryId).resultId(resultId);
        var request = new BulkDeleteResultsDto().results(List.of(item));
        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(request));

        assertThat(exception.getCode())
                .isEqualTo(
                        ApplicationListEntryResultError.APPLICATION_ENTRY_RESULT_NOT_WITHIN_ENTRY);
    }
}
