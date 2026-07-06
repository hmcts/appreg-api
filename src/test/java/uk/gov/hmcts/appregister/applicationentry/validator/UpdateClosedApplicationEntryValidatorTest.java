package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;

class UpdateClosedApplicationEntryValidatorTest {
    private final ApplicationListEntryRepository entryRepository =
            mock(ApplicationListEntryRepository.class);
    private final ApplicationListRepository listRepository = mock(ApplicationListRepository.class);
    private final UpdateClosedApplicationEntryValidator validator =
            new UpdateClosedApplicationEntryValidator(entryRepository, listRepository);

    @Test
    void validate_whenListMissing_thenThrows() {
        var payload = payload();
        when(listRepository.findByUuidIncludingDelete(payload.getId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(payload));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST);
    }

    @Test
    void validate_whenEntryMissing_thenThrows() {
        var payload = payload();
        when(listRepository.findByUuidIncludingDelete(payload.getId()))
                .thenReturn(Optional.of(mock(ApplicationList.class)));
        when(entryRepository.findByUuid(payload.getEntryId())).thenReturn(Optional.empty());

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(payload));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.ENTRY_DOES_NOT_EXIST);
    }

    @Test
    void validate_whenEntryNotWithinList_thenThrows() {
        var payload = payload();
        when(listRepository.findByUuidIncludingDelete(payload.getId()))
                .thenReturn(Optional.of(mock(ApplicationList.class)));
        when(entryRepository.findByUuid(payload.getEntryId()))
                .thenReturn(Optional.of(mock(ApplicationListEntry.class)));
        when(entryRepository.findByEntryUuidWithinListUuid(payload.getId(), payload.getEntryId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(payload));

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.ENTRY_IS_NOT_WITHIN_LIST);
    }

    @Test
    void validate_whenListNotClosed_thenThrows() {
        var payload = payload();
        var list = mock(ApplicationList.class);
        when(list.getStatus()).thenReturn(Status.OPEN);
        var entry = mock(ApplicationListEntry.class);
        when(listRepository.findByUuidIncludingDelete(payload.getId()))
                .thenReturn(Optional.of(list));
        when(entryRepository.findByUuid(payload.getEntryId())).thenReturn(Optional.of(entry));
        when(entryRepository.findByEntryUuidWithinListUuid(payload.getId(), payload.getEntryId()))
                .thenReturn(Optional.of(entry));

        var exception = assertThrows(AppRegistryException.class, () -> validator.validate(payload));

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_MUST_BE_CLOSED);
    }

    @Test
    void validate_whenValid_thenReturnsCallbackValue() {
        var payload = payload();
        var list = mock(ApplicationList.class);
        when(list.getStatus()).thenReturn(Status.CLOSED);
        var entry = mock(ApplicationListEntry.class);
        when(listRepository.findByUuidIncludingDelete(payload.getId()))
                .thenReturn(Optional.of(list));
        when(entryRepository.findByUuid(payload.getEntryId())).thenReturn(Optional.of(entry));
        when(entryRepository.findByEntryUuidWithinListUuid(payload.getId(), payload.getEntryId()))
                .thenReturn(Optional.of(entry));

        String result =
                validator.validate(
                        payload,
                        (validatable, success) -> {
                            assertThat(success.getApplicationList()).isSameAs(list);
                            assertThat(success.getApplicationEntryId()).isSameAs(entry);
                            return "ok";
                        });

        verify(entryRepository).findByUuid(payload.getEntryId());
        assertThat(result).isEqualTo("ok");
    }

    private static PayloadForUpdateClosedEntry payload() {
        return new PayloadForUpdateClosedEntry(
                new EntryUpdateClosedDto(), UUID.randomUUID(), UUID.randomUUID());
    }
}
