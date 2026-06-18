package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.PayloadForUpdateClosedEntry;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.generated.model.EntryUpdateClosedDto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateClosedApplicationEntryValidatorTest {

    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @Mock private ApplicationListRepository applicationListRepository;

    @InjectMocks private UpdateClosedApplicationEntryValidator validator;

    private UUID listId;
    private UUID entryId;
    private ApplicationList applicationList;
    private ApplicationListEntry applicationListEntry;
    private EntryUpdateClosedDto entryUpdateClosedDto;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        applicationList = new ApplicationList();
        applicationList.setStatus(Status.CLOSED);

        applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setNotes("existing notes");

        entryUpdateClosedDto = new EntryUpdateClosedDto();
        entryUpdateClosedDto.setAdditionalNotes("additional notes");

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuid(entryId))
                .thenReturn(Optional.of(applicationListEntry));
        when(applicationListEntryRepository.findByEntryUuidWithinListUuid(listId, entryId))
                .thenReturn(Optional.of(applicationListEntry));
    }

    @Test
    void givenCombinedNotesAre4000Characters_whenValidate_thenSucceeds() {
        applicationListEntry.setNotes("a".repeat(3998));
        entryUpdateClosedDto.setAdditionalNotes("b");

        UpdateApplicationEntryClosedValidationSuccess success =
                validator.validate(payload(), (request, validated) -> validated);

        Assertions.assertEquals(applicationList, success.getApplicationList());
        Assertions.assertEquals(applicationListEntry, success.getApplicationEntryId());
    }

    @Test
    void givenCombinedNotesExceed4000Characters_whenValidate_thenBadRequestIsThrown() {
        applicationListEntry.setNotes("a".repeat(3999));
        entryUpdateClosedDto.setAdditionalNotes("b");

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload()));

        Assertions.assertEquals(AppListEntryError.NOTES_TOO_LONG, exception.getCode());
        Assertions.assertEquals(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                exception.getCode().getCode().getHttpCode());
        Assertions.assertTrue(exception.getMessage().contains("4000 character limit"));
    }

    @Test
    void givenListDoesNotExist_whenValidate_thenEntryIsNotChecked() {
        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.empty());

        AppRegistryException exception =
                Assertions.assertThrows(
                        AppRegistryException.class, () -> validator.validate(payload()));

        Assertions.assertEquals(
                AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST, exception.getCode());
        verify(applicationListEntryRepository, never()).findByUuid(entryId);
    }

    private PayloadForUpdateClosedEntry payload() {
        return new PayloadForUpdateClosedEntry(entryUpdateClosedDto, listId, entryId);
    }
}
