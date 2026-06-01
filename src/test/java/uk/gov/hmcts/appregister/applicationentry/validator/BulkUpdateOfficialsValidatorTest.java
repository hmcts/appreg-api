package uk.gov.hmcts.appregister.applicationentry.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.appregister.applicationentry.exception.AppListEntryError;
import uk.gov.hmcts.appregister.applicationentry.model.BulkUpdateOfficialsPayload;
import uk.gov.hmcts.appregister.applicationlist.exception.ApplicationListError;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListEntryRepository;
import uk.gov.hmcts.appregister.common.entity.repository.ApplicationListRepository;
import uk.gov.hmcts.appregister.common.enumeration.Status;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.data.AppListTestData;
import uk.gov.hmcts.appregister.generated.model.BulkOfficialsUpdateDto;
import uk.gov.hmcts.appregister.generated.model.Official;
import uk.gov.hmcts.appregister.generated.model.OfficialType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkUpdateOfficialsValidatorTest {

    @Mock private ApplicationListRepository applicationListRepository;
    @Mock private ApplicationListEntryRepository applicationListEntryRepository;

    @InjectMocks private BulkUpdateOfficialsValidator validator;

    private UUID listId;
    private UUID entryId;
    private ApplicationList applicationList;
    private ApplicationListEntry applicationListEntry;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        applicationList = new AppListTestData().someMinimal().status(Status.OPEN).build();
        applicationList.setDeleted(null);

        applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setUuid(entryId);

        when(applicationListRepository.findByUuidIncludingDelete(listId))
                .thenReturn(Optional.of(applicationList));
        when(applicationListEntryRepository.findByUuidsInSourceList(listId, Set.of(entryId)))
                .thenReturn(List.of(applicationListEntry));
    }

    @Test
    void validateWithoutCallback_whenPayloadIsValid_thenCompletes() {
        validator.validate(validPayload(entryId));
    }

    @Test
    void validateWithCallback_whenPayloadIsValid_thenReturnsMatchingEntries() {
        List<ApplicationListEntry> entries =
                validator.validate(
                        validPayload(entryId), (request, success) -> success.getEntries());

        assertThat(entries).containsExactly(applicationListEntry);
    }

    @Test
    void validate_whenApplicationListDoesNotExist_thenThrowsApplicationListDoesNotExist() {
        UUID missingListId = UUID.randomUUID();
        when(applicationListRepository.findByUuidIncludingDelete(missingListId))
                .thenReturn(Optional.empty());
        BulkUpdateOfficialsPayload payload = validPayload(missingListId, entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_DOES_NOT_EXIST);
    }

    @Test
    void validate_whenApplicationListIsClosed_thenThrowsApplicationListStateIsIncorrect() {
        applicationList.setStatus(Status.CLOSED);
        BulkUpdateOfficialsPayload payload = validPayload(entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT);
    }

    @Test
    void validate_whenApplicationListIsDeleted_thenThrowsApplicationListStateIsIncorrect() {
        applicationList.setDeleted(true);
        BulkUpdateOfficialsPayload payload = validPayload(entryId);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode())
                .isEqualTo(AppListEntryError.APPLICATION_LIST_STATE_IS_INCORRECT);
    }

    @Test
    void validate_whenEntryIdsAreMissing_thenThrowsEntryNotProvided() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId, new BulkOfficialsUpdateDto().officials(validOfficials()));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_PROVIDED);
    }

    @Test
    void validate_whenEntryIdsAreEmpty_thenThrowsEntryNotProvided() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of())
                                .officials(validOfficials()));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_PROVIDED);
    }

    @Test
    void validate_whenOfficialsAreMissing_thenThrowsOfficialsNotProvided() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId, new BulkOfficialsUpdateDto().entryIds(List.of(entryId)));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIALS_NOT_PROVIDED);
    }

    @Test
    void validate_whenPayloadDataIsNull_thenThrowsEntryNotProvided() {
        BulkUpdateOfficialsPayload payload = new BulkUpdateOfficialsPayload(listId, null);

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_PROVIDED);
    }

    @Test
    void validate_whenOfficialTitleIsBlank_thenThrowsOfficialTitleRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(List.of(official(OfficialType.MAGISTRATE).title(" "))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_TITLE_REQUIRED);
    }

    @Test
    void validate_whenOfficialForenameIsBlank_thenThrowsOfficialForenameRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(official(OfficialType.MAGISTRATE).forename(" "))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_FORENAME_REQUIRED);
    }

    @Test
    void validate_whenOfficialSurnameIsBlank_thenThrowsOfficialSurnameRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(Set.of(entryId))
                                .officials(
                                        List.of(official(OfficialType.MAGISTRATE).surname(" "))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_SURNAME_REQUIRED);
    }

    @Test
    void validate_whenMultipleOfficialsHaveBlankTitle_thenReportsAllInvalidIndexes() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(
                                                official(OfficialType.MAGISTRATE).title(" "),
                                                official(OfficialType.CLERK),
                                                official(OfficialType.MAGISTRATE).title(" "))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_TITLE_REQUIRED);
        assertThat(exception.getMessage()).contains("[0, 2]");
    }

    @Test
    void validate_whenOfficialTitleIsNull_thenThrowsOfficialTitleRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(List.of(official(OfficialType.MAGISTRATE).title(null))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_TITLE_REQUIRED);
    }

    @Test
    void validate_whenOfficialForenameIsNull_thenThrowsOfficialForenameRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(official(OfficialType.MAGISTRATE).forename(null))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_FORENAME_REQUIRED);
    }

    @Test
    void validate_whenOfficialSurnameIsNull_thenThrowsOfficialSurnameRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(official(OfficialType.MAGISTRATE).surname(null))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_SURNAME_REQUIRED);
    }

    @Test
    void validate_whenOfficialTypeIsMissing_thenThrowsOfficialTypeRequired() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(Arrays.asList(null, official(null))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.OFFICIAL_TYPE_REQUIRED);
    }

    @Test
    void validate_whenTooManyMagistrates_thenThrowsTooManyMagistrates() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(
                                                official(OfficialType.MAGISTRATE),
                                                official(OfficialType.MAGISTRATE),
                                                official(OfficialType.MAGISTRATE),
                                                official(OfficialType.MAGISTRATE))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.TOO_MANY_MAGISTRATES);
    }

    @Test
    void validate_whenTooManyCourtOfficials_thenThrowsTooManyCourtOfficials() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId))
                                .officials(
                                        List.of(
                                                official(OfficialType.CLERK),
                                                official(OfficialType.CLERK))));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(AppListEntryError.TOO_MANY_COURT_OFFICIALS);
    }

    @Test
    void validate_whenSomeEntriesAreNotInSourceList_thenThrowsEntryNotInSourceList() {
        UUID missingEntryId = UUID.randomUUID();
        when(applicationListEntryRepository.findByUuidsInSourceList(
                        listId, List.of(entryId, missingEntryId)))
                .thenReturn(List.of(applicationListEntry));
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId, missingEntryId))
                                .officials(validOfficials()));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_NOT_IN_SOURCE_LIST);
    }

    @Test
    void validate_whenEntryIdsContainDuplicates_thenThrowsEntryIdsMustBeUnique() {
        BulkUpdateOfficialsPayload payload =
                new BulkUpdateOfficialsPayload(
                        listId,
                        new BulkOfficialsUpdateDto()
                                .entryIds(List.of(entryId, entryId))
                                .officials(validOfficials()));

        AppRegistryException exception = validateAndCapture(payload);

        assertThat(exception.getCode()).isEqualTo(ApplicationListError.ENTRY_IDS_MUST_BE_UNIQUE);
    }

    private BulkUpdateOfficialsPayload validPayload(UUID entryId) {
        return validPayload(listId, entryId);
    }

    private BulkUpdateOfficialsPayload validPayload(UUID listId, UUID entryId) {
        return new BulkUpdateOfficialsPayload(
                listId,
                new BulkOfficialsUpdateDto()
                        .entryIds(List.of(entryId))
                        .officials(validOfficials()));
    }

    private AppRegistryException validateAndCapture(BulkUpdateOfficialsPayload payload) {
        return assertThrows(AppRegistryException.class, () -> validator.validate(payload));
    }

    private List<Official> validOfficials() {
        return List.of(official(OfficialType.MAGISTRATE), official(OfficialType.CLERK));
    }

    private Official official(OfficialType type) {
        return new Official().title("Mr").forename("Ada").surname("Bench").type(type);
    }
}
